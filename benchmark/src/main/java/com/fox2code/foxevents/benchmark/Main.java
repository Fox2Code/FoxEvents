package com.fox2code.foxevents.benchmark;

public final class Main {
    public static void main(String[] args) {
        FoxEventsBenchmark foxEventsBenchmark = new FoxEventsBenchmark();
        System.out.println("Running warm up");
        foxEventsBenchmark.runBenchmark();
        foxEventsBenchmark.runBenchmark(new BenchmarkSampleEvent());
        foxEventsBenchmark.runBenchmarkOptimal();
        Runtime.getRuntime().gc();
        try {
            System.out.println("Giving JVM time to JIT benchmark...");
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
            return;
        }
        BenchmarkResults benchmarkResults = new BenchmarkResults();
        System.out.println("Executing benchmark for about 5seconds");
        int count = 32;
        long millisLine = System.currentTimeMillis() + 5000L;
        while (count-->0 || (millisLine - System.currentTimeMillis()) > 0) {
            runBenchmarks(foxEventsBenchmark, benchmarkResults);
            if (count == -1) count = 0;
        }
        System.out.println("Benchmark results: " + nsToMsStr(benchmarkResults.nsBenchmark2) + " | " +
                nsToMsStr(benchmarkResults.nsBenchmarkSme2) + " | " + nsToMsStr(benchmarkResults.nsBenchmarkOpt2));
    }

    private static void runBenchmarks(FoxEventsBenchmark foxEventsBenchmark, BenchmarkResults benchmarkResults) {
        System.out.println("Running un-cached callEvent, new event benchmark");
        benchmarkResults.addBenchmark(foxEventsBenchmark.runBenchmark());
        System.out.println("Running un-cached callEvent, singleton event benchmark");
        benchmarkResults.addBenchmarkSme(foxEventsBenchmark.runBenchmark(new BenchmarkSampleEvent()));
        System.out.println("Running cached callEvent, new event benchmark");
        benchmarkResults.addBenchmarkOpt(foxEventsBenchmark.runBenchmarkOptimal());
    }

    static String nsToMsStr(long nanos) {
        return (nanos / 1000000D) + "ms";
    }

    static class BenchmarkResults {
        long nsBenchmark1 = Long.MAX_VALUE;
        long nsBenchmark2 = Long.MAX_VALUE;
        long nsBenchmarkSme1 = Long.MAX_VALUE;
        long nsBenchmarkSme2 = Long.MAX_VALUE;
        long nsBenchmarkOpt1 = Long.MAX_VALUE;
        long nsBenchmarkOpt2 = Long.MAX_VALUE;

        void addBenchmark(long nsBenchmark) {
            if (this.nsBenchmark1 > nsBenchmark) {
                this.nsBenchmark2 = this.nsBenchmark1;
                this.nsBenchmark1 = nsBenchmark;
            } else  if (this.nsBenchmark2 > nsBenchmark) {
                this.nsBenchmark2 = nsBenchmark;
            }
        }

        void addBenchmarkSme(long nsBenchmarkSme) {
            if (this.nsBenchmarkSme1 > nsBenchmarkSme) {
                this.nsBenchmarkSme2 = this.nsBenchmarkSme1;
                this.nsBenchmarkSme1 = nsBenchmarkSme;
            } else  if (this.nsBenchmarkSme2 > nsBenchmarkSme) {
                this.nsBenchmarkSme2 = nsBenchmarkSme;
            }
        }

        void addBenchmarkOpt(long nsBenchmarkOpt) {
            if (this.nsBenchmarkOpt1 > nsBenchmarkOpt) {
                this.nsBenchmarkOpt2 = this.nsBenchmarkOpt1;
                this.nsBenchmarkOpt1 = nsBenchmarkOpt;
            } else  if (this.nsBenchmarkOpt2 > nsBenchmarkOpt) {
                this.nsBenchmarkOpt2 = nsBenchmarkOpt;
            }
        }
    }
}