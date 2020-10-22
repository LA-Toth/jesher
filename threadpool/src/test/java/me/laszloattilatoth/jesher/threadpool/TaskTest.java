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
import org.junit.jupiter.api.Test;

public class TaskTest {
    private static class TestTask extends Task {
        public int anInt = 3;
        TestTask(ThreadPool pool) {
            super(pool);

        }

        @Override
        protected void doRun() {
            anInt=42;
        }

        @Override
        public ThreadPool pool () {
            return super.pool();
        }
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

    private final TestThreadPool pool = new TestThreadPool(2);
    private final TestTask task = new TestTask(pool);

    @Test
    void testDoRunIsCalled() {
        Assertions.assertEquals(3, task.anInt);
        task.run();
        Assertions.assertEquals(42, task.anInt);
    }

    @Test
    void testPoolIsThePassedOne() {
        Assertions.assertEquals(this.pool, this.task.pool());
    }

    @Test
    void testCompletionIsTrackedInTask() {
        Assertions.assertFalse(task.isCompleted());
        task.run();
        Assertions.assertTrue(task.isCompleted());
    }

    @Test
    void testThatPoolIsNotified() {
        pool.task = task;
        Assertions.assertFalse(pool.notified);
        task.run();
        Assertions.assertTrue(pool.notified);
    }
}
