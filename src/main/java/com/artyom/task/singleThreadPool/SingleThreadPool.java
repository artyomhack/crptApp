package com.artyom.task.singleThreadPool;

import java.util.ArrayList;

public class SingleThreadPool {

    private static Thread backThread;
    private BlockingQueue blockingQueue = new BlockingQueue();
    static class BlockingQueue {
        private final ArrayList<Runnable> threads = new ArrayList<>();

        public synchronized Runnable get() {
            while (threads.isEmpty()) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            var run = threads.get(0);
            threads.remove(run);
            return run;
        }

        public synchronized void put(Runnable run) {
            threads.add(run);
            notify();
        }
    }

    public void start() {
        backThread = new Thread( () -> {
            while (!Thread.currentThread().isInterrupted())
                blockingQueue.get().run();
        });

        backThread.setPriority(Thread.MAX_PRIORITY);
        backThread.start();
    }

    public void submit(Runnable run) {
        blockingQueue.put(run);
    }

    public void stop() {
        while (backThread.isAlive()) {
            try {
                backThread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void terminate() {
        backThread.interrupt();
    }
}
