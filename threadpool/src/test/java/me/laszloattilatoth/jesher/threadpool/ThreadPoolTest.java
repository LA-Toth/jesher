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
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ThreadPoolTest implements ValueStore {
    private static final List<TestTaskConfig> EMPTY_CONFIG_LIST = new ArrayList<>();
    private final TestThreadPool pool = new TestThreadPool(10);
    private final List<Integer> values = new ArrayList<>();

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
    void testTaskStartingOtherTasksAfterItself() {
        List<TestTaskConfig> nextTasks = new ArrayList<>();
        nextTasks.add(new TestTaskConfig(2));
        nextTasks.add(new TestTaskConfig(3));

        addTaskAndWaitForCompletion(new TestTaskConfig(40, EMPTY_CONFIG_LIST, nextTasks));
        assertValuesCount(3);
        assertEquals(40, values.get(0));
        assertTrue(values.get(1) == 2 || values.get(1) == 3);
        assertTrue(values.get(2) == 2 || values.get(2) == 3);
        assertNotEquals(values.get(1), values.get(2));
    }

    @Test
    void testTaskStartingOtherTaskInParalell() {
        List<TestTaskConfig> parallelTasks = new ArrayList<>();
        parallelTasks.add(new TestTaskConfig(2));
        parallelTasks.add(new TestTaskConfig(3));

        addTaskAndWaitForCompletion(new TestTaskConfig(40, parallelTasks));
        assertValuesCount(3);
        assertThat(values.get(0), anyOf(is(40), is(2), is(3)));
        assertThat(values.get(1), anyOf(is(40), is(2), is(3)));
        assertThat(values.get(2), anyOf(is(40), is(2), is(3)));
        assertNotEquals(values.get(0), values.get(1));
        assertNotEquals(values.get(0), values.get(2));
        assertNotEquals(values.get(1), values.get(2));
    }

    @Test
    void testPostProcessor() {
        List<TestTaskConfig> post = new ArrayList<>();
        post.add(new TestTaskConfig(42));
        addTaskAndWaitForCompletion(new TestTaskConfig(40, EMPTY_CONFIG_LIST, EMPTY_CONFIG_LIST, post));
        assertValuesCount(2);
        assertEquals(40, values.get(0));
        assertEquals(42, values.get(1));
    }

    @Test
    void testComplexNonParallel() {
        final int count = 1;
        for (int i = 0; i != count; ++i) {
            pool.add(new TestTask(pool, this, create1()));
        }

        try {
            pool.waitAllTask();
        } catch (InterruptedException e) {
            throw new AssertionError("Should not be reached");
        }

        List<Integer> expected = new ArrayList<>();
        for (int i = 0; i != count; ++i) {
            expected.add(1);
        }
        for (int i = 0; i != count * 5; ++i) {
            expected.add(2);
        }
        for (int i = 0; i != count * 5 * 4; ++i) {
            expected.add(3);
        }
        for (int i = 0; i != count * 5; ++i) {
            expected.add(4);
        }
        for (int i = 0; i != count; ++i) {
            expected.add(5);
        }
        // unfortunately due to parallel run it's impossible to ensure the exact order
        assertEquals(new HashSet<>(expected), new HashSet<>(values));
    }

    TestTaskConfig create1() {
        final int count = 5;
        List<TestTaskConfig> nList = new ArrayList<>();
        List<TestTaskConfig> pList = new ArrayList<>();
        for (int i = 0; i != count; ++i)
            nList.add(create2());
        pList.add(create4());
        return new TestTaskConfig(1, EMPTY_CONFIG_LIST, nList, pList);
    }

    TestTaskConfig create2() {
        final int count = 4;
        List<TestTaskConfig> nList = new ArrayList<>();
        List<TestTaskConfig> pList = new ArrayList<>();
        for (int i = 0; i != count; ++i)
            nList.add(create3());
        pList.add(create5());
        return new TestTaskConfig(2, EMPTY_CONFIG_LIST, nList, pList);
    }

    TestTaskConfig create3() {
        return new TestTaskConfig(3);
    }

    TestTaskConfig create4() {
        return new TestTaskConfig(4);
    }

    TestTaskConfig create5() {
        return new TestTaskConfig(5);
    }

    private static class TestThreadPool extends ThreadPool {
        public boolean notified = false;
        public Task task = null;

        TestThreadPool(int threadCount) {
            super(threadCount);
        }

        @Override
        void taskCompleted(Task t) {
            super.taskCompleted(t);
            notified = task != null && task == t;
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
        final List<TestTaskConfig> post;

        TestTaskConfig(int value, List<TestTaskConfig> parallel, List<TestTaskConfig> after, List<TestTaskConfig> post) {
            this.value = value;
            this.parallel = parallel;
            this.after = after;
            this.post = post;
        }

        TestTaskConfig(int value, List<TestTaskConfig> parallel, List<TestTaskConfig> after) {
            this(value, parallel, after, new ArrayList<>());
        }

        TestTaskConfig(int value, List<TestTaskConfig> parallel) {
            this(value, parallel, new ArrayList<>(), new ArrayList<>());
        }

        TestTaskConfig(int value) {
            this(value, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
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

            // register tasks (which won't be started before current one finishes)
            for (TestTaskConfig afterCfg : config.after) {
                this.addNextTask(new TestTask(pool(), store, afterCfg));
            }
            for (TestTaskConfig postCfg : config.post) {
                this.addPostProcessorTask(new TestTask(pool(), store, postCfg));
            }

            // ---> parallel run starts here
            for (TestTaskConfig parallelCfg : config.parallel) {
                pool().addTask(new TestTask(pool(), store, parallelCfg));
            }
            // ensure some randomness
            try {
                Thread.sleep(Math.round(Math.random() * 1000) + 100);
            } catch (InterruptedException e) {
                // nothing to do here
            }
            store.addValue(config.value);
            // <--- parallel run ends here

            try {
                Thread.sleep(Math.round(Math.random() * 1000) + 100);
            } catch (InterruptedException e) {
                // nothing to do here
            }
        }
    }
}
