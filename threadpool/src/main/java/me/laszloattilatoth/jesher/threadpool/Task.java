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
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Task implements Runnable {
    private final AtomicBoolean completed = new AtomicBoolean(false);
    private final WeakReference<ThreadPool> pool;

    public Task(ThreadPool pool) {
        this.pool = new WeakReference<>(pool);
        this.completed.set(false);
    }

    public boolean isCompleted() {
        return completed.get();
    }

    protected ThreadPool pool() {
        return this.pool.get();
    }

    @Override
    public void run() {
        try {
            doRun();
        } finally {
            completed.set(true);
            notifyPool();
        }
    }

    protected abstract void doRun();

    protected void addNextTask(Task t) {
        pool().addTaskAfter(t, this);
    }

    protected void addNextRunnable(Runnable r) {
        pool().addAfter(r, this);
    }

    protected void addTask(Task t) {
        pool().addTask(t);
    }

    protected void addRunnable(Runnable r) {
        pool().add(r);
    }

    private void notifyPool() {
        pool().taskCompleted(this);
    }
}
