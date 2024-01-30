package com.artyom.api.task2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.time.LocalTime;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

public class CrptApiSecond {
    private final Semaphore semaphore;
    private final Object lock = new Object();
    private final long initialDelay;

    public CrptApiSecond(int requestLimit, int periodLimit, TimeUnit timeUnit) {
        semaphore = new Semaphore(requestLimit);
        initialDelay = timeUnit.toMillis(periodLimit);
        new Thread(() -> {
            while (true) {
                if (semaphore.availablePermits() == 0) {
                    try {
                        releasePermits(requestLimit);
                        Thread.sleep(initialDelay);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                synchronized (lock) {
                    try {
                        lock.wait(initialDelay);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }).start();
    }

    private void releasePermits(int permits) {
        semaphore.drainPermits();
        semaphore.release(permits);
    }

    public CompletableFuture<Boolean> createDocument() {
        System.out.println("Try to acquire " + semaphore.availablePermits() + " permits at " + LocalTime.now());
        try {
            semaphore.acquire();
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return sendRequest();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private Boolean sendRequest() throws InterruptedException {
        var random = new Random();
        System.out.println("Send request at " + LocalTime.now());
        var requestTime = random.nextInt(1000) + 500;
        Thread.sleep(requestTime);
        System.out.println("Got response at " + LocalTime.now() + " after " + requestTime + " ms");
        return true;
    }
}

class MySecondTest {

    @TestFactory
    Collection<DynamicTest> createLimitedRequestTest() {
        var random = new Random();
        return Stream.generate(() -> {
            var count = random.nextInt(300) + 100;
            var requestLimit = random.nextInt(30) + 10;
            var periodSecLimit = random.nextInt(15) + 1;
            var name = String.format("Creating %d documents, limits %d docs per %d s", count, requestLimit, periodSecLimit);
            return DynamicTest.dynamicTest(name, () -> {
                createDocumentRequests(count, requestLimit, periodSecLimit);
            });
        }).limit(10).toList();
    }

    private void createDocumentRequests(int count, int requestLimit, int periodSecLimit) {
        var my = new CrptApiSecond(requestLimit, periodSecLimit, TimeUnit.SECONDS);
        var start = System.currentTimeMillis();
        System.out.println("Start at " + LocalTime.now());
        Stream.generate(my::createDocument)
                .limit(count).count();
        var end = System.currentTimeMillis();
        System.out.println("End at " + LocalTime.now() + " after " + (end - start) + " ms");
        var expectedTime = (count + 1) / 100 * 1000;
        Assertions.assertTrue(end - start > expectedTime);
    }
}
