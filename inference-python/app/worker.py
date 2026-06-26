import asyncio
import json
import logging
from confluent_kafka import Consumer
import redis
from opentelemetry import trace
from opentelemetry.trace.propagation.tracecontext import TraceContextTextMapPropagator
from app.contracts import TelemetryPayloadSchema

logger = logging.getLogger("WorkerLogger")
tracer = trace.get_tracer("foundry-async-worker")

# Establish a connection to an in-memory Redis database for tracking duplicate delivery IDs
r_client = redis.Redis(host="redis-service", port=6379, db=0, decode_responses=True)

kafka_config = {
    'bootstrap.servers': 'kafka-cluster-kafka-bootstrap:9092',
    'group.id': 'foundry-analytics-group',
    'auto.offset.reset': 'earliest',
    'enable.auto.commit': False # Manually commit offsets only after safe processing execution
}

async def start_event_consumer():
    consumer = Consumer(kafka_config)
    consumer.subscribe(['foundry-outbox-events'])

    try:
        while True:
            msg = consumer.poll(timeout=0.5)
            if msg is None:
                await asyncio.sleep(0.05)
                continue

            raw_payload = json.loads(msg.value().decode('utf-8'))
            event_id = raw_payload.get("id")
            traceparent = raw_payload.get("traceparent")

            # Re-verify and stitch back the OpenTelemetry Context across language environments
            context = TraceContextTextMapPropagator().extract(carrier={"traceparent": traceparent}) if traceparent else None

            with tracer.start_as_current_span("compute_telemetry_event", context=context) as span:
                span.set_attribute("event.id", event_id)

                # ATOMIC CHECK-AND-SET: Use Redis to drop duplicate payloads right away
                if not r_client.setnx(f"processed_evt:{event_id}", "true"):
                    logger.warning(f"Duplicate event encountered: {event_id}. Drop operation executed.")
                    span.set_attribute("execution.idempotent_skip", True)
                    consumer.commit(msg, asynchronous=False)
                    continue

                # Set a 7-day TTL on event tracking IDs to manage memory consumption
                r_client.expire(f"processed_evt:{event_id}", 604800)

                try:
                    structured_data = TelemetryPayloadSchema.parse_raw_outbox(raw_payload.get("payload"))
                    logger.info(f"Successfully processed machine event: {structured_data.id}")
                    consumer.commit(msg, asynchronous=False)
                except Exception as ex:
                    logger.error(f"Execution processing breakdown on key {event_id}: {ex}")
                    span.record_exception(ex)
                    r_client.delete(f"processed_evt:{event_id}") # Evict key to allow retry if the process fails
    finally:
        consumer.close()