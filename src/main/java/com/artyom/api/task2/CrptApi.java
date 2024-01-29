package com.artyom.api.task2;

import com.artyom.api.task1.Document;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Time;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {
    private static final String URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final TimeUnit timeUnit;
    private final long periodLimit;
    private final int requestLimit;
    private final long initialDelay;

    private final Semaphore semaphore;
    private final SingleThreadPool singleThreadPool;

    public CrptApi(long periodLimit, int requestLimit, TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.periodLimit = periodLimit;
        initialDelay = timeUnit.toSeconds(periodLimit);
        semaphore = new Semaphore(requestLimit);
        singleThreadPool = new SingleThreadPool();
        singleThreadPool.submit(() -> clearingSemaphoreByInterval(initialDelay, periodLimit, timeUnit));
    }

    public void clearingSemaphoreByInterval(long initialDelay, long periodLimit, TimeUnit timeUnit) {
        var current = System.currentTimeMillis();
        var end = System.currentTimeMillis() + periodLimit;

    }

    public Future<Boolean> sendDocument(Document document) {

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
