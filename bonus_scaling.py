import time
from multiprocessing import Pool
import threading
import csv
from pathlib import Path

def cpu_work(n):
    count = 0
    for i in range(n):
        count += i * i
    return count

def run_threads(chunks):
    results = [None] * len(chunks)
    def work(i, n): results[i] = cpu_work(n)
    threads = [threading.Thread(target=work, args=(i, n)) for i, n in enumerate(chunks)]
    start = time.perf_counter()
    for t in threads: t.start()
    for t in threads: t.join()
    return results, time.perf_counter() - start

def run_processes(chunks):
    start = time.perf_counter()
    with Pool(len(chunks)) as pool:
        results = pool.map(cpu_work, chunks)
    return results, time.perf_counter() - start

if __name__ == "__main__":
    rows = []
    total_work = 50_000_000
    for workers in [1, 2, 4, 8, 16, 32]:
        q, r = divmod(total_work, workers)
        chunks = [q + (i < r) for i in range(workers)]
        a, t = run_threads(chunks)
        b, m = run_processes(chunks)
        assert a == b
        rows.append((workers, t, m))
        print(f"Workers: {workers} | Threads: {t:.8f}s | Processes: {m:.8f}s", flush=True)
    with open(Path(__file__).with_name('bonus_results_230103360.csv'), 'w', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(['workers', 'threading_seconds', 'multiprocessing_seconds'])
        writer.writerows(rows)
