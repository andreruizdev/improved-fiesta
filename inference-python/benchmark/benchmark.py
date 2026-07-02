import time
import asyncio
import sys
import os
import numpy as np
from numba import jit
from concurrent.futures import ProcessPoolExecutor, ThreadPoolExecutor

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '../app')))
from model import model_instance

@jit(nopython=True)
def heavy_matrix_work(n):
    A = np.random.rand(n, n)
    B = np.random.rand(n, n)
    C = np.dot(A, B.T)
    return C

def cpu_bound_task(amount, term):
    # Predict first
    prob, shap_vals = model_instance.predict(amount, term)
    heavy_matrix_work(50)
    return prob, shap_vals

async def mock_event_stream_sync(num_events=20):
    start = time.time()
    for i in range(num_events):
        prob, shap_vals = cpu_bound_task(amount=5000 + i, term=12 + (i % 5))
    end = time.time()
    return end - start

async def mock_event_stream_async_pool(num_events=20, executor=None):
    loop = asyncio.get_running_loop()
    start = time.time()
    tasks = []
    for i in range(num_events):
        task = loop.run_in_executor(
            executor,
            cpu_bound_task,
            5000 + i,
            12 + (i % 5)
        )
        tasks.append(task)
    await asyncio.gather(*tasks)
    end = time.time()
    return end - start

async def run_benchmarks():
    print("Warming up Numba...")
    heavy_matrix_work(10)

    num_events = 20
    print(f"\n--- Running benchmark with {num_events} events ---")

    print("\nRunning baseline (Synchronous blocking)...")
    start_time = time.time()
    duration_sync = await mock_event_stream_sync(num_events)
    sync_throughput = num_events / duration_sync
    print(f"Sync Total time: {duration_sync:.4f} seconds")
    print(f"Sync Throughput: {sync_throughput:.2f} events/sec")

    print("\nRunning optimized (Async with ThreadPoolExecutor)...")
    with ThreadPoolExecutor(max_workers=4) as executor:
        start_time = time.time()
        duration_async = await mock_event_stream_async_pool(num_events, executor)
        async_throughput = num_events / duration_async
        print(f"Async Total time: {duration_async:.4f} seconds")
        print(f"Async Throughput: {async_throughput:.2f} events/sec")

    print("\n--- Summary ---")
    speedup = async_throughput / sync_throughput
    print(f"Throughput improvement: {speedup:.2f}x")

    with open("benchmark_results.txt", "w") as f:
        f.write(f"Baseline Throughput: {sync_throughput:.2f} events/sec\n")
        f.write(f"Optimized Throughput: {async_throughput:.2f} events/sec\n")
        f.write(f"Speedup: {speedup:.2f}x\n")

if __name__ == "__main__":
    asyncio.run(run_benchmarks())
