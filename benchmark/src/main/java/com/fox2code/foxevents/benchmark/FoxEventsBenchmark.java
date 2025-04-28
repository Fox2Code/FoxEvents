package com.fox2code.foxevents.benchmark;

import com.fox2code.foxevents.EventHandler;
import com.fox2code.foxevents.EventHolder;
import com.fox2code.foxevents.FoxEvents;

/**
 * Class that manage FoxEvents benchmarking
 * @since 1.2.0
 */
public final class FoxEventsBenchmark {
    public final int benchmarkTimes;
    public final long warmUpNanos;
    private final EventHolder<BenchmarkSampleEvent> benchmarkSampleEventEventHolder;

    public FoxEventsBenchmark() {
        this(1000000);
    }

    public FoxEventsBenchmark(int benchmarkTimes) {
        long startNanos = System.nanoTime();
        this.benchmarkTimes = benchmarkTimes;
        this.benchmarkSampleEventEventHolder =
                EventHolder.getHolderFromEvent(BenchmarkSampleEvent.class);
        this.runBenchmarkImpl(null, 1);
        this.runBenchmarkImpl(null, 10);
        this.warmUpNanos = System.nanoTime() - startNanos;
    }

    public long runBenchmark() {
        long startNanos = System.nanoTime();
        this.runBenchmarkImpl(null, this.benchmarkTimes);
        return System.nanoTime() - startNanos;
    }

    public long runBenchmark(BenchmarkSampleEvent benchmarkSampleEvent) {
        long startNanos = System.nanoTime();
        this.runBenchmarkImpl(benchmarkSampleEvent, this.benchmarkTimes);
        return System.nanoTime() - startNanos;
    }

    public long runBenchmarkOptimal() {
        long startNanos = System.nanoTime();
        this.runBenchmarkOptimalImpl(null, this.benchmarkTimes);
        return System.nanoTime() - startNanos;
    }

    public long runBenchmarkOptimal(BenchmarkSampleEvent benchmarkSampleEvent) {
        long startNanos = System.nanoTime();
        this.runBenchmarkOptimalImpl(benchmarkSampleEvent, this.benchmarkTimes);
        return System.nanoTime() - startNanos;
    }

    private void runBenchmarkImpl(BenchmarkSampleEvent benchmarkSampleEvent, int count) {
        TestHandler.INSTANCE.counter = benchmarkSampleEvent != null &&
                benchmarkSampleEvent.isCancelled() ? 0 : -count;
        TestHandler.INSTANCE.cancelledCounter = -count;
        while (count-->0) {
            (benchmarkSampleEvent == null ? new BenchmarkSampleEvent() : benchmarkSampleEvent).callEvent();
        }
        if (TestHandler.INSTANCE.counter != 0 || TestHandler.INSTANCE.cancelledCounter != 0) {
            throw new RuntimeException("Counter issue!");
        }
    }

    private void runBenchmarkOptimalImpl(BenchmarkSampleEvent benchmarkSampleEvent, int count) {
        TestHandler.INSTANCE.counter = benchmarkSampleEvent != null &&
                benchmarkSampleEvent.isCancelled() ? 0 : -count;
        TestHandler.INSTANCE.cancelledCounter = -count;
        while (count-->0) {
            this.benchmarkSampleEventEventHolder.callEvent(
                    benchmarkSampleEvent == null ? new BenchmarkSampleEvent() : benchmarkSampleEvent);
        }
        if (TestHandler.INSTANCE.counter != 0 || TestHandler.INSTANCE.cancelledCounter != 0) {
            throw new RuntimeException("Counter issue!");
        }
    }

    public static class TestHandler {
        static TestHandler INSTANCE = new TestHandler();

        static {
            FoxEvents.getFoxEvents().registerEvents(TestHandler.INSTANCE);
            FoxEvents.getFoxEvents().registerEvents(PassiveHandler.class);
        }

        private TestHandler() {}

        private long counter = 0, cancelledCounter = 0;

        @EventHandler
        public void onReceiveEvent(BenchmarkSampleEvent event) {
            this.counter++;
        }

        @EventHandler(ignoreCancelled = true)
        public void onReceiveEventCancelled(BenchmarkSampleEvent event) {
            this.cancelledCounter++;
        }

        public static class PassiveHandler {
            @EventHandler
            public static void onReceiveEvent(BenchmarkSampleEvent event) {}

            @EventHandler(ignoreCancelled = true)
            public static void onReceiveEventCancelled(BenchmarkSampleEvent event) {}
        }
    }
}
