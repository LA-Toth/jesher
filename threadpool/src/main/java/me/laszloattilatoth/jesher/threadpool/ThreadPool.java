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

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ThreadPool {
    private final TaskManager taskManager = new TaskManager(this);
    private final ExecutorService executor;

    public ThreadPool(int threads) {
        this.executor = Executors.newFixedThreadPool(threads);
    }

    public void add(Runnable r) {
        if (r instanceof Task)
            addTask((Task) r);
        else
            addTask(new RunnableTask(r, this));
    }

    public void addTask(Task t) {
        taskManager.addTask(t);
        submitTask(t);
    }

    public void addAfter(Runnable r, Task previous) {
        if (r instanceof Task)
            addTaskAfter((Task) r, previous);
        else
            addTaskAfter(new RunnableTask(r, this), previous);
    }

    public void addTaskAfter(Task t, Task previous) {
        taskManager.addTaskAfter(t, previous);
    }

    public void waitAllTask() throws InterruptedException {
        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }

    void taskCompleted(Task t) {
        taskManager.removeTask(t);
    }

    void submitTask(Task t) {
        executor.submit(t);
    }

    private static class TaskManager {
        private final Set<Task> tasks = new HashSet<>();
        private final Map<Task, Set<Task>> tasksAfter = new HashMap<>();
        private final Map<Task, Set<Task>> taskDeps = new HashMap<>();
        private final WeakReference<ThreadPool> pool;

        private TaskManager(ThreadPool pool) {this.pool = new WeakReference<>(pool);}

        public synchronized void addTask(Task t) {
            addTaskNoSync(t);
        }

        private void addTaskNoSync(Task t) {
            if (tasks.contains(t))
                return;

            tasks.add(t);
            tasksAfter.put(t, new HashSet<>());
            taskDeps.put(t, new HashSet<>());
        }

        synchronized void addTaskAfter(Task t, Task previousTask) {
            addTaskNoSync(t);
            tasksAfter.get(previousTask).add(t);
            taskDeps.get(t).add(previousTask);
        }

        synchronized void removeTask(Task t) {
            tasks.remove(t);
            for (Task followingTask : tasksAfter.get(t)) {
                taskDeps.get(followingTask).remove(t);
                if (canStartNoSync(followingTask))
                    Objects.requireNonNull(pool.get()).submitTask(followingTask);
            }
        }

        private boolean canStartNoSync(Task t) {
            return taskDeps.get(t).size() == 0;
        }
    }
}
