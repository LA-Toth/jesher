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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPool {
    public static int CPU_THREADS = Runtime.getRuntime().availableProcessors();
    private final TaskManager taskManager = new TaskManager(this);
    private final ThreadPoolExecutor executor;

    public ThreadPool() {
        this(Math.max(1, CPU_THREADS));
    }

    public ThreadPool(int threads) {
        this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threads);
    }

    public int threadCount() {
        return executor.getCorePoolSize();
    }

    public boolean has(Runnable r) {
        if (r instanceof Task)
            return hasTask((Task) r);
        else
            return false;
    }

    public boolean hasTask(Task t) {
        return taskManager.hasTask(t);
    }

    public void add(Runnable r) {
        if (r instanceof Task)
            addTask((Task) r);
        else
            addTask(new RunnableTask(r, this));
    }

    public void addTask(Task t) {
        taskManager.addTask(t);
        executor.submit(t);
    }

    void add(Runnable r, Task previous) {
        if (r instanceof Task)
            addTask((Task) r, previous);
        else
            addTask(new RunnableTask(r, this), previous);
    }

    void addTask(Task t, Task previous) {
        taskManager.addTask(t, previous);
    }

    void addPostProcessor(Runnable r, Task currentTask) {
        if (r instanceof Task)
            addPostProcessorTask((Task) r, currentTask);
        else
            addPostProcessorTask(new RunnableTask(r, this), currentTask);
    }

    void addPostProcessorTask(Task t, Task currentTask) {
        taskManager.addPostProcessorTask(t, currentTask);
    }

    public void waitAllTask() throws InterruptedException {
        synchronized (taskManager) {
            while (taskManager.hasRemainingTask())
                taskManager.wait();
        }
        shutdown();
    }

    public void shutdown() throws InterruptedException {
        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
    }

    void taskCompleted(Task t) {
        taskManager.taskCompleted(t);
    }

    void submitTask(Task t) {
        executor.submit(t);
    }

    private static class TaskManager {
        private final Set<Task> tasks = new HashSet<>();
        private final Set<Task> completedTasks = new HashSet<>();
        private final Map<Task, Set<Task>> taskChildren = new HashMap<>();
        private final Map<Task, Task> taskParent = new HashMap<>();
        private final Map<Task, Set<Task>> taskPostProcessors = new HashMap<>();
        private final WeakReference<ThreadPool> pool;

        private TaskManager(ThreadPool pool) {
            this.pool = new WeakReference<>(pool);
            this.taskChildren.put(null, new HashSet<>());
        }

        public synchronized boolean hasTask(Task t) {
            return tasks.contains(t);
        }

        public synchronized void addTask(Task t) {
            addTaskNoSync(t, null);
        }

        public synchronized void addTask(Task t, Task parent) {
            addTaskNoSync(t, parent);
        }

        public void addTaskNoSync(Task t, Task parent) {
            if (tasks.contains(t))
                return;

            tasks.add(t);
            taskChildren.put(t, new HashSet<>());
            taskPostProcessors.put(t, new HashSet<>());
            taskParent.put(t, parent);
            taskChildren.get(parent).add(t);
        }

        public synchronized void addPostProcessorTask(Task t, Task currentTask) {
            taskPostProcessors.get(currentTask).add(t);
        }

        synchronized void taskCompleted(Task t) {
            completedTasks.add(t);
            if (hasChildren(t)) {
                startNextTasks(t);
            } else if (hasPostProcessors(t)) {
                startPostProcessorTasks(t);
            } else {
                finishTask(t);
            }

            if (!hasRemainingTask())
                notifyAll();
        }

        private void startNextTasks(Task t) {
            for (Task followingTask : taskChildren.get(t)) {
                Objects.requireNonNull(pool.get()).submitTask(followingTask);
            }
        }

        private void startPostProcessorTasks(Task t) {
            for (Task pp : taskPostProcessors.get(t)) {
                addTaskNoSync(pp, t);
                Objects.requireNonNull(pool.get()).submitTask(pp);
            }
        }

        private void finishTask(Task t) {
            if (!completedTasks.contains(t))
                return;
            tasks.remove(t);
            completedTasks.remove(t);
            Task parent = t;

            while (taskParent.containsKey(parent)) {
                Task current = parent;
                parent = taskParent.get(current);
                taskParent.remove(current);
                Set<Task> pps = taskPostProcessors.get(current);
                taskPostProcessors.remove(current);
                assert taskChildren.get(current).size() == 0;
                taskChildren.remove(current);
                taskChildren.get(parent).remove(current);

                if (parent == null)
                    break;

                Set<Task> postProcessors = taskPostProcessors.get(parent);

                boolean wasPostProcessorTask = postProcessors.contains(current);

                if (wasPostProcessorTask) {
                    postProcessors.remove(current);
                }

                if (hasChildren(parent)) {
                    // still have submitted tasks, exit from loop to avoid check of postprocessors
                    // in current or parent tasks
                    break;
                }

                if (!wasPostProcessorTask) {
                    // started by startNextTasks(), and no further sibling nextTask
                    if (hasPostProcessors(parent)) {
                        startPostProcessorTasks(parent);
                        break;
                    } else {
                        tasks.remove(parent);
                        completedTasks.remove(parent);
                    }
                } else {
                    tasks.remove(parent);
                    completedTasks.remove(parent);
                }
            }
        }

        private boolean hasChildren(Task t) {
            return taskChildren.get(t).size() > 0;
        }

        private boolean hasPostProcessors(Task t) {
            return taskPostProcessors.get(t).size() > 0;
        }

        public boolean hasRemainingTask() {
            return tasks.size() > 0;
        }
    }
}
