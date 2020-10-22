/*
 * Copyright 2020 Laszlo Attila Toth
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
