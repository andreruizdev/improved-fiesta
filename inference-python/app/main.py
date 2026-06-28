import os
import json
import asyncio
import logging
from fastapi import FastAPI, BackgroundTasks
import redis
import redis.asyncio as redis_async
from confluent_kafka import Consumer, KafkaError
from opentelemetry import trace
from opentelemetry.trace.propagation.tracecontext import TraceContextTextMapPropagator

from schemas import LoanApplicationSchema
from model import model_instance

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="Risk Inference Service")

REDIS_HOST = os.getenv("REDIS_HOST", "localhost")
REDIS_PORT = int(os.getenv("REDIS_PORT", "6379"))
redis_client = redis.Redis(host=REDIS_HOST, port=REDIS_PORT, db=0, decode_responses=True)

# Async Redis for performance-critical path
async_redis_pool = redis_async.ConnectionPool(host=REDIS_HOST, port=REDIS_PORT, db=0, decode_responses=True, max_connections=100)
async_redis_client = redis_async.Redis(connection_pool=async_redis_pool)

KAFKA_BROKER = os.getenv("KAFKA_BROKER", "localhost:9092")
kafka_conf = {
    'bootstrap.servers': KAFKA_BROKER,
    'group.id': 'risk-inference-group',
    'auto.offset.reset': 'earliest'
}

tracer = trace.get_tracer(__name__)

async def process_kafka_messages():
    consumer = Consumer(kafka_conf)
    consumer.subscribe(['loan-applications'])
    
    logger.info("Started Kafka consumer")
    
    try:
        while True:
            msg = consumer.poll(timeout=1.0)
            if msg is None:
                await asyncio.sleep(0.1)
                continue
            if msg.error():
                if msg.error().code() == KafkaError._PARTITION_EOF:
                    continue
                else:
                    logger.error(f"Kafka error: {msg.error()}")
                    continue

            raw_val = msg.value().decode('utf-8')
            event_data = json.loads(raw_val)
            
            event_id = event_data.get("id")
            payload_str = event_data.get("payload")
            traceparent = event_data.get("traceparent")
            
            ctx = None
            if traceparent:
                carrier = {"traceparent": traceparent}
                ctx = TraceContextTextMapPropagator().extract(carrier=carrier)
            
            with tracer.start_as_current_span("process_loan_event", context=ctx) as span:
                span.set_attribute("event_id", event_id)
                
                if not await async_redis_client.setnx(f"processed_event:{event_id}", "1"):
                    logger.info(f"Event {event_id} already processed. Skipping.")
                    span.set_attribute("idempotency_skip", True)
                    continue
                
                await async_redis_client.expire(f"processed_event:{event_id}", 604800)

                try:
                    loan_app = LoanApplicationSchema.from_outbox_json(payload_str)
                    prob, shap_vals = model_instance.predict(loan_app.amount, loan_app.termMonths)
                    
                    logger.info(f"Processed loan {loan_app.id}: Default Prob={prob:.4f}")
                    span.set_attribute("loan_id", loan_app.id)
                    span.set_attribute("default_probability", prob)
                    
                except Exception as e:
                    logger.error(f"Failed to process event {event_id}: {e}")
                    span.record_exception(e)
                    await async_redis_client.delete(f"processed_event:{event_id}")

    finally:
        consumer.close()

@app.on_event("startup")
async def startup_event():
    asyncio.create_task(process_kafka_messages())

@app.get("/health")
def health_check():
    return {"status": "healthy"}
