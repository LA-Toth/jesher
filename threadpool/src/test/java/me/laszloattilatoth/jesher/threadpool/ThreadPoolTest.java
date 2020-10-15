package me.laszloattilatoth.jesher.threadpool;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolTest {
    private static class TestThreadPool extends ThreadPool {

        TestThreadPool(int threadCount) {
            super(threadCount);
        }
    }

    private static class CountingRunnable implements Runnable {
        public static AtomicInteger count = new AtomicInteger(0);

        public void run() {
            count.set(count.get() + 1);
        }

        public static void reset() {
            count.set(0);
        }
    }

    private final TestThreadPool pool = new TestThreadPool(10);

    @BeforeEach
    void setUp() {
        CountingRunnable.reset();
    }

    @Test
    void testThreadCountsAreTheSpecifiedValues() {
        Assertions.assertEquals(2, new ThreadPool(2).threadCount());
        Assertions.assertEquals(42, new ThreadPool(42).threadCount());
    }

    @Test
    void testDefaultThreadCountIsTheCPUThreadCount() {
        Assertions.assertEquals(Runtime.getRuntime().availableProcessors(), new ThreadPool().threadCount());
        Assertions.assertEquals(Runtime.getRuntime().availableProcessors(), ThreadPool.CPU_THREADS);
    }

    @Test
    void testMultiRunnable() {
        Assertions.assertEquals(0, CountingRunnable.count.get());

        int count = 40;
        for (int i = 0; i != count; ++i)
            pool.add(new CountingRunnable());

        try {
            pool.waitAllTask();
        } catch (InterruptedException e) {
            throw new AssertionError("Should not be reached");
        }
        Assertions.assertEquals(count, CountingRunnable.count.get());
    }
}
