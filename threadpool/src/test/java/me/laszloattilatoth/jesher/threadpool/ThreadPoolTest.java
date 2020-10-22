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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ThreadPoolTest implements ValueStore {
    private static final List<TestTaskConfig> EMPTY_CONFIG_LIST = new ArrayList<>();
    private final TestThreadPool pool = new TestThreadPool(10);
    private List<Integer> values = new ArrayList<>();

    public synchronized void addValue(int value) {
        values.add(value);
    }

    private void addTaskAndWaitForCompletion(TestTaskConfig config) {
        TestTask task = new TestTask(pool, this, config);
        assertEquals(0, values.size());
        pool.add(task);

        try {
            pool.waitAllTask();
        } catch (InterruptedException e) {
            throw new AssertionError("Should not be reached");
        }
    }

    private void assertValuesCount(int expectedCount) {
        assertEquals(expectedCount, values.size());
    }

    @BeforeEach
    void setUp() {
        CountingRunnable.reset();
    }

    @Test
    void testThreadCountsAreTheSpecifiedValues() {
        assertEquals(2, new ThreadPool(2).threadCount());
        assertEquals(42, new ThreadPool(42).threadCount());
    }

    @Test
    void testDefaultThreadCountIsTheCPUThreadCount() {
        assertEquals(Runtime.getRuntime().availableProcessors(), new ThreadPool().threadCount());
        assertEquals(Runtime.getRuntime().availableProcessors(), ThreadPool.CPU_THREADS);
    }

    @Test
    void testThatPoolWithoutTasksTerminates() {
        try {
            new ThreadPool().waitAllTask();
        } catch (InterruptedException e) {
            throw new AssertionError("Should not be reached");
        }
        assertTrue(true);
    }

    @Test
    void testMultiRunnable() {
        assertEquals(0, CountingRunnable.count.get());

        int count = 40;
        for (int i = 0; i != count; ++i)
            pool.add(new CountingRunnable());

        try {
            pool.waitAllTask();
        } catch (InterruptedException e) {
            throw new AssertionError("Should not be reached");
        }
        assertEquals(count, CountingRunnable.count.get());
    }

    @Test
    void testThatALongRunningRunnableLetStopThePool() {

        pool.add(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                // noop
            }
        });

        try {
            pool.shutdown();
        } catch (InterruptedException e) {
            throw new AssertionError("Should not be reached");
        }
        assertTrue(true);
    }

    @Test
    void testSingleTaskCanStoreValue() {
        addTaskAndWaitForCompletion(new TestTaskConfig(40));
        assertValuesCount(1);
        assertEquals(40, values.get(0));
    }

    @Test
    void testTaskStartingOtherTasks() {
        List<TestTaskConfig> nextTasks = new ArrayList<>();
        nextTasks.add(new TestTaskConfig(2));
        nextTasks.add(new TestTaskConfig(3));

        addTaskAndWaitForCompletion(new TestTaskConfig(40, nextTasks, EMPTY_CONFIG_LIST));
        assertValuesCount(3);
        assertEquals(40, values.get(0));
        assertTrue(values.get(1) == 2 || values.get(1) == 3);
        assertTrue(values.get(2) == 2 || values.get(2) == 3);
        assertNotEquals(values.get(1), values.get(2));
    }

    private static class TestThreadPool extends ThreadPool {

        TestThreadPool(int threadCount) {
            super(threadCount);
        }
    }

    private static class CountingRunnable implements Runnable {
        public static AtomicInteger count = new AtomicInteger(0);

        public static void reset() {
            count.set(0);
        }

        public void run() {
            try {
                Thread.sleep(Math.round(Math.random() * 1000) + 100);
            } catch (InterruptedException e) {
                // nothing to do here
            }
            count.incrementAndGet();
        }
    }

    private static class TestTaskConfig {
        final int value;
        final List<TestTaskConfig> parallel;
        final List<TestTaskConfig> after;

        TestTaskConfig(int value, List<TestTaskConfig> parallel, List<TestTaskConfig> after) {
            this.value = value;
            this.parallel = parallel;
            this.after = after;
        }

        TestTaskConfig(int value) {
            this(value, new ArrayList<>(), new ArrayList<>());
        }
    }

    private static class TestTask extends Task {
        private final ValueStore store;
        private final TestTaskConfig config;

        public TestTask(ThreadPool pool, ValueStore store, TestTaskConfig config) {
            super(pool);
            this.store = store;
            this.config = config;
        }

        @Override
        protected void doRun() {
            try {
                Thread.sleep(Math.round(Math.random() * 1000) + 100);
            } catch (InterruptedException e) {
                // nothing to do here
            }
            store.addValue(config.value);

            for (TestTaskConfig nextCfg : config.parallel) {
                pool().addTask(new TestTask(pool(), store, nextCfg));
            }
            try {
                Thread.sleep(Math.round(Math.random() * 1000) + 100);
            } catch (InterruptedException e) {
                // nothing to do here
            }
        }
    }
}
