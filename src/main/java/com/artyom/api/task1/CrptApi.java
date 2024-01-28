package com.artyom.api.task1;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class CrptApi {
    private final static String URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final static ObjectMapper MAPPER = new ObjectMapper();
    private final Semaphore semaphore;
    public CrptApi(int requestLimit, int periodLimit, TimeUnit timeUnit) {
        semaphore = new Semaphore(periodLimit);
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        executorService.scheduleAtFixedRate(() -> releasePermits(requestLimit), timeUnit.toSeconds(periodLimit), periodLimit, timeUnit);
    }

    private void releasePermits(int permits) {
        semaphore.drainPermits();
        semaphore.release(permits);
    }

    public CompletableFuture<Boolean> sendDocument(Document document) {
        System.out.println("Try to acquire " + semaphore.availablePermits() + " permits at " + LocalTime.now());
        try {
            semaphore.acquire();
            return CompletableFuture.supplyAsync( () -> doRequest(document));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private Boolean doRequest(Document document) {
        try {
            HttpURLConnection urlConnection = (HttpURLConnection) new URL(URL).openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setDoOutput(true);
            writeBodyToStream(document, urlConnection.getOutputStream());
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeBodyToStream(Document document, OutputStream stream) throws IOException {
        var json = MAPPER.writeValueAsString(document);
        var writer = new OutputStreamWriter(stream);
        writer.write(json);
        writer.flush();
    }
}
