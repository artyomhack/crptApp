package com.artyom.api.task2;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalTime;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CrptApiSecond {
    private static final String URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final int requestLimit;
    private final Semaphore semaphore;
    private final long periodLimit;
    private final Future futureInterval;
    private final TimeUnit timeUnit;
    private final Lock lock = new ReentrantLock();
    private long initialDelay;


    public CrptApiSecond(int requestLimit, long periodLimit, TimeUnit timeUnit) {
        this.requestLimit = requestLimit;
        this.periodLimit = periodLimit;
        this.timeUnit = timeUnit;
        initialDelay = timeUnit.toMillis(periodLimit);
        semaphore = new Semaphore(requestLimit);
        futureInterval = CompletableFuture.runAsync(this::makeInterval);
    }

    public void makeInterval() {
        try {
            while (true) {
                System.out.println("Start period...");
                lock.wait(periodLimit * 1000);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void cleanSemaphore(int permits) {
        System.out.println("Clear semaphore started");
        semaphore.drainPermits();
        semaphore.release(permits);
        System.out.println("Clear semaphore finished");
    }

    public CompletableFuture<Boolean> sendDocument() {
        System.out.println("Try to acquire " + semaphore.availablePermits() + " permits at " + LocalTime.now());
        try {
            semaphore.acquire();
            return CompletableFuture.supplyAsync(this::doRequest);
        } catch (InterruptedException  e) {
            throw new RuntimeException(e);
        }
    }

    private Boolean doRequest() {
        try {
            var random = new Random();
            System.out.println("Send request at " + LocalTime.now());
            var requestTime = random.nextInt(1000) + 500;
            Thread.sleep(requestTime);
            System.out.println("Got response at " + LocalTime.now() + " after " + requestTime + " ms");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return true;
    }
}
