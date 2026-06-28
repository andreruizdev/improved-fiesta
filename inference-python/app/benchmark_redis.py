import asyncio
import time
import uuid
import logging
from redis import Redis
from redis.asyncio import Redis as AsyncRedis
from redis.asyncio import ConnectionPool

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("benchmark")

def run_sync_benchmark(num_ops: int):
    client = Redis(host="localhost", port=6379, db=0, decode_responses=True)
    client.ping() # Ensure connection

    start_time = time.perf_counter()
    # Pipelining sync for fair comparison or regular? Let's use pipeline as it's common practice for multi commands.
    for _ in range(num_ops):
        event_id = str(uuid.uuid4())
        client.setnx(f"sync:{event_id}", "1")
        client.expire(f"sync:{event_id}", 60)
        client.delete(f"sync:{event_id}") # cleanup

    end_time = time.perf_counter()
    duration = end_time - start_time
    logger.info(f"Sync Benchmark (No Pipeline): {num_ops} ops in {duration:.4f}s ({num_ops/duration:.2f} ops/s)")
    return duration

async def run_async_pipeline_benchmark(num_ops: int):
    pool = ConnectionPool(host="localhost", port=6379, db=0, decode_responses=True, max_connections=5000)
    client = AsyncRedis(connection_pool=pool)
    await client.ping() # Ensure connection

    async def task():
        event_id = str(uuid.uuid4())
        pipe = client.pipeline(transaction=False)
        pipe.setnx(f"async_p:{event_id}", "1")
        pipe.expire(f"async_p:{event_id}", 60)
        pipe.delete(f"async_p:{event_id}") # cleanup
        await pipe.execute()

    start_time = time.perf_counter()

    batch_size = 500
    for i in range(0, num_ops, batch_size):
        tasks = [task() for _ in range(min(batch_size, num_ops - i))]
        await asyncio.gather(*tasks)

    end_time = time.perf_counter()
    duration = end_time - start_time
    logger.info(f"Async Benchmark (pipeline): {num_ops} ops in {duration:.4f}s ({num_ops/duration:.2f} ops/s)")
    await client.aclose()
    return duration

async def main():
    num_ops = 5000
    logger.info(f"Starting benchmark with {num_ops} operations each...")

    sync_time = run_sync_benchmark(num_ops)
    async_pipeline_time = await run_async_pipeline_benchmark(num_ops)

    improvement = ((sync_time - async_pipeline_time) / sync_time) * 100
    if async_pipeline_time < sync_time:
        logger.info(f"Async pipeline is {improvement:.2f}% faster than Sync")
    else:
        logger.info(f"Sync is {-improvement:.2f}% faster than Async pipeline")

if __name__ == "__main__":
    asyncio.run(main())
