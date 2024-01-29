package com.artyom.api.task2;

import java.util.ArrayList;

public class SingleThreadPool {

    private Thread backThread;

    private final BlockingQueue blockingQueue;

    public SingleThreadPool() {
        blockingQueue = new BlockingQueue(1);
    }

    public SingleThreadPool(int countThreads) {
        blockingQueue = new BlockingQueue(countThreads);
    }

    static class BlockingQueue {
        private final ArrayList<Runnable> threads;

        public BlockingQueue(int requestLimit) {
            threads = new ArrayList<>(requestLimit);
        }

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
