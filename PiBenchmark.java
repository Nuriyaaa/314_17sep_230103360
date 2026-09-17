import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.SplittableRandom;
import java.util.Arrays;

public class PiBenchmark {
    static long totalHits = 0;
    static final Object LOCK = new Object();

    enum Mode { UNSAFE, SYNCHRONIZED, LOCAL }

    static class Result {
        final long hits;
        final double ms;
        Result(long hits, long start) {
            this.hits = hits;
            this.ms = (System.nanoTime() - start) / 1_000_000.0;
        }
        double pi(long points) { return 4.0 * hits / points; }
    }

    static long count(long points, long seed, Mode mode) {
        SplittableRandom random = new SplittableRandom(seed);
        long localHits = 0;
        for (long i = 0; i < points; i++) {
            double x = random.nextDouble();
            double y = random.nextDouble();
            if (x * x + y * y <= 1.0) {
                if (mode == Mode.UNSAFE) {
                    totalHits++; // Deliberate data race required by Part 1.
                } else if (mode == Mode.SYNCHRONIZED) {
                    synchronized (LOCK) { totalHits++; }
                } else {
                    localHits++;
                }
            }
        }
        return localHits;
    }

    static Result serial(long points, long seed) {
        long start = System.nanoTime();
        long hits = count(points, seed, Mode.LOCAL);
        return new Result(hits, start);
    }

    static Result parallel(long points, int threads, Mode mode, long seed)
            throws InterruptedException {
        totalHits = 0;
        long[] partial = new long[threads];
        Thread[] workers = new Thread[threads];
        long start = System.nanoTime();
        for (int i = 0; i < threads; i++) {
            final int index = i;
            final long share = points / threads + (i < points % threads ? 1 : 0);
            workers[i] = new Thread(() ->
                partial[index] = count(share, seed + index, mode));
        }
        for (Thread worker : workers) worker.start();
        for (Thread worker : workers) worker.join();
        long hits = totalHits;
        if (mode == Mode.LOCAL) {
            hits = 0;
            for (long value : partial) hits += value;
        }
        return new Result(hits, start);
    }

    static double median(double[] times) {
        double[] sorted = times.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US);
        boolean quick = args.length > 0 && args[0].equals("--quick");
        long n = quick ? 100_000L : 50_000_000L;
        long reductionN = quick ? 200_000L : 100_000_000L;
        String filename = quick ? "SMOKE_TEST.md" : "RESULTS.md";
        try (PrintWriter out = new PrintWriter(filename, StandardCharsets.UTF_8.name())) {
            out.println("# Monte Carlo pi benchmark" + (quick ? " (SMOKE TEST ONLY)" : ""));
            out.println("\nGenerated from actual execution on this machine.");
            out.println("\nDate: " + java.time.ZonedDateTime.now());
            out.println("\nOS: " + System.getProperty("os.name") + " " + System.getProperty("os.arch"));
            out.println("\nJava: " + System.getProperty("java.version"));
            out.println("\nAvailable logical processors: " + Runtime.getRuntime().availableProcessors());
            out.println("\nPhysical core count and CPU model: record these from your system information.");
            out.println("\nMethod: System.nanoTime; computation includes random number generation. Parallel timings include thread creation, start, join and final summation. File output is excluded. Each worker has its own SplittableRandom. Parts 2 and 3 use three trials and median time. Short warm-ups are excluded. This is a classroom benchmark, not a rigorous JVM performance study.");
            System.out.println("Warming up...");
            for (int i = 0; i < 3; i++) {
                serial(200_000, i);
                for (Mode mode : Mode.values()) parallel(200_000, 4, mode, i);
            }
            out.println("\n## Part 1: shared counter without synchronization");
            out.println("\nTotal points per run: " + n + "; threads: 4.");
            out.println("\n| Run | Pi | Time (ms) |\n|---|---|---|");
            for (int run = 1; run <= 5; run++) {
                Result r = parallel(n, 4, Mode.UNSAFE, 1000L * run);
                out.printf("| %d | %.8f | %.3f |%n", run, r.pi(n), r.ms);
            }
            out.println("\nThe increment is a read-modify-write operation, not an atomic operation. Concurrent updates may be lost. The size of the error varies; a particular incorrect pi range is not guaranteed.");
            System.out.println("Part 1 finished. Running synchronized comparison...");
            out.println("\n## Part 2: synchronization versus a plain single-thread loop");
            out.println("\nTotal points per trial: " + n + ".");
            out.println("\n| Trial | Serial pi | Serial ms | Synchronized pi (4 threads) | Synchronized ms |\n|---|---|---|---|---|");
            double[] serialTimes = new double[3];
            double[] syncTimes = new double[3];
            for (int trial = 0; trial < 3; trial++) {
                Result a, b;
                if (trial % 2 == 0) {
                    a = serial(n, 2000L + trial);
                    b = parallel(n, 4, Mode.SYNCHRONIZED, 2000L + trial);
                } else {
                    b = parallel(n, 4, Mode.SYNCHRONIZED, 2000L + trial);
                    a = serial(n, 2000L + trial);
                }
                serialTimes[trial] = a.ms;
                syncTimes[trial] = b.ms;
                out.printf("| %d | %.8f | %.3f | %.8f | %.3f |%n", trial + 1, a.pi(n), a.ms, b.pi(n), b.ms);
            }
            out.printf("%nMedian serial: %.3f ms; median synchronized: %.3f ms; synchronized/serial time ratio: %.3fx.%n", median(serialTimes), median(syncTimes), median(syncTimes) / median(serialTimes));
            System.out.println("Part 2 finished. Running reduction...");
            out.println("\n## Part 3: local counters and final reduction");
            out.println("\nTotal points per trial: " + reductionN + ", divided across all workers.");
            out.println("\n| Threads | Trial 1 ms | Trial 2 ms | Trial 3 ms | Median ms | Speedup | Efficiency | Last trial pi |\n|---|---|---|---|---|---|---|---|");
            double baseline = 0;
            for (int threads : new int[] {1, 2, 4, 8, 16, 32}) {
                parallel(200_000, threads, Mode.LOCAL, 42);
                double[] times = new double[3];
                Result r = null;
                for (int trial = 0; trial < 3; trial++) {
                    r = parallel(reductionN, threads, Mode.LOCAL, 3000L + trial);
                    times[trial] = r.ms;
                }
                double time = median(times);
                if (threads == 1) baseline = time;
                double speedup = baseline / time;
                out.printf("| %d | %.3f | %.3f | %.3f | %.3f | %.3fx | %.2f%% | %.8f |%n", threads, times[0], times[1], times[2], time, speedup, 100 * speedup / threads, r.pi(reductionN));
                System.out.println("Finished " + threads + " threads.");
            }
            out.println("\nSpeedup = T1 / Tn. Efficiency = speedup / number of threads * 100%. The one-thread baseline in Part 3 uses the same worker-thread implementation as the other rows.");
            out.println("\n## Questions");
            out.println("\n### 1. Why do 16 threads not run twice as fast as 8 on an 8-core CPU?");
            out.println("\nSixteen software threads do not create sixteen physical cores. Once the physical cores are busy, extra threads share execution resources. Simultaneous multithreading may help, but does not double the hardware resources. Scheduling, thread startup and joining also cost time. Therefore, speedup can flatten or decrease. An actual machine may have a different core count or a mix of performance and efficiency cores; interpret the measured rows using its hardware.");
            out.println("\n### 2. Why can the synchronized version be slower than one core?");
            out.println("\nEvery successful point requires entry into the same critical section. Only one thread can update the counter at a time, and frequent lock acquisition, contention and cache-coherence traffic add overhead. The serial loop updates a private counter without this coordination. Local counters remove coordination from the hot loop; the main thread combines the partial results after join. The exact slowdown depends on the JVM and machine; no fixed percentage of stalled cycles was measured here.");
        }
        System.out.println("Report saved to " + filename);
    }
}
