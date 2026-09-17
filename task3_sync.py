import threading
import time

TOTAL_OPS = 2_000_000
NUM_THREADS = 4

class UnsafeCounter:
    def __init__(self): self.val = 0
    def inc(self): self.val += 1

class LockedCounter:
    def __init__(self):
        self.val = 0
        self.lock = threading.Lock()
    def inc(self):
        with self.lock:
            self.val += 1

def bench(counter_type):
    c = counter_type()
    ops_per_thread = TOTAL_OPS // NUM_THREADS
    def work():
        for _ in range(ops_per_thread): c.inc()
    threads = [threading.Thread(target=work) for _ in range(NUM_THREADS)]
    start = time.perf_counter()
    for t in threads: t.start()
    for t in threads: t.join()
    return c.val, time.perf_counter() - start

def bench_local():
    partials = [0] * NUM_THREADS
    def work(index, count):
        local = 0
        for _ in range(count):
            local += 1
        partials[index] = local
    quotient, remainder = divmod(TOTAL_OPS, NUM_THREADS)
    threads = [threading.Thread(target=work, args=(i, quotient + (i < remainder)))
               for i in range(NUM_THREADS)]
    start = time.perf_counter()
    for t in threads: t.start()
    for t in threads: t.join()
    value = sum(partials)
    elapsed = time.perf_counter() - start
    assert value == TOTAL_OPS, (value, TOTAL_OPS)
    return value, elapsed

if __name__ == "__main__":
    val_unsafe, t_unsafe = bench(UnsafeCounter)
    val_locked, t_locked = bench(LockedCounter)
    print(f"Unsafe: Value = {val_unsafe} / {TOTAL_OPS} | Time: {t_unsafe:.8f}s")
    print(f"Locked: Value = {val_locked} / {TOTAL_OPS} | Time: {t_locked:.8f}s")
    print(f"Lost updates: {TOTAL_OPS-val_unsafe}")
    print(f"Contention Cost Multiplier: {t_locked/t_unsafe:.8f}x")
    val_local, t_local = bench_local()
    print(f"Thread-local: Value = {val_local} | Time: {t_local:.8f}s")
    print(f"Speedup over locked: {t_locked/t_local:.8f}x")
    print(f"Meets 2x requirement in this run: {t_locked/t_local >= 2.0}")
