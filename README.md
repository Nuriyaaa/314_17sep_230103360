# Java Monte Carlo benchmark

This project covers all three parts of the assignment in PiBenchmark.java.
Requires a JDK (Java 8 or newer), including javac. No external libraries.

## Run on your own computer

Open a terminal in this folder:

With Java 11 or newer, a single command is enough:

```sh
java PiBenchmark.java
```

Alternatively, compile and run:

```sh
javac PiBenchmark.java
java PiBenchmark
```

The program writes RESULTS.md with actual measurements, the five unsafe runs,
the synchronized comparison, the reduction table (1, 2, 4, 8, 16, 32 threads),
and answers to both questions. Existing RESULTS.md is replaced on each run.
Keep the laptop plugged in and close heavy applications before measuring.
Add your CPU model and physical core count to the report using system information.
Available logical processors is not necessarily the physical core count.

Part 1 and Part 2 use 50,000,000 total points per run.
Part 3 uses 100,000,000 total points per run, not per thread.
Three trials are measured in Parts 2 and 3; the table uses median runtime.
An unsafe value near real pi is possible: races have no guaranteed error size.

Optional quick check (does not satisfy the benchmark iteration requirements):

```sh
java PiBenchmark --quick
```

## Files to submit

- PiBenchmark.java
- README.md
- RESULTS.md generated on your computer

Do not submit .class files or SMOKE_TEST.md as benchmark results.
Upload to the benchmark Git repository before class ends and grant access
to @sufyanism as required by the assignment.

## Краткое объяснение

- UNSAFE: четыре потока меняют один счётчик без защиты; обновления теряются.
- SYNCHRONIZED: общий счётчик защищён, но потоки конкурируют за одну блокировку.
- LOCAL: каждый поток считает отдельно; после join главный поток складывает результаты.
- join ждёт завершения потока и позволяет корректно прочитать его результат.
- Попадание: x*x + y*y <= 1. Оценка pi: 4.0 * hits / points.

Замеры нужно выполнить на своём компьютере. Готовые значения не выдуманы
и не подставлены: таблицы появятся после запуска программы.
