package com.artyom.api.task2;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.time.LocalTime;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class CrptApiSecond {
    private final Semaphore semaphore;
    private final int requestLimit;
    private final long periodLimit;

    public CrptApiSecond(int requestLimit, int periodLimit, TimeUnit timeUnit) {
        this.requestLimit = requestLimit;
        this.periodLimit = timeUnit.toMillis(periodLimit);
        semaphore = new Semaphore(requestLimit);
    }

    private void releasePermits(int permits) {
        semaphore.drainPermits();
        semaphore.release(permits);
    }

    public CompletableFuture<Boolean> createDocument() {
        System.out.println("Try to acquire " + semaphore.availablePermits() + " permits at " + LocalTime.now());
        try {
            semaphore.acquire();
            new Thread(() -> {
                var endLimitMs = System.currentTimeMillis() + periodLimit;
                while (true) {
                    if (endLimitMs > System.currentTimeMillis()) {
                        try {
                            Thread.sleep(periodLimit);
                            if (semaphore.availablePermits() == 0)
                                releasePermits(requestLimit);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    endLimitMs = System.currentTimeMillis() + periodLimit;
                }
            }).start();

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
