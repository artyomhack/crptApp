package com.artyom.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;


public class CrptApi {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final Long limitMs;

    private volatile long startPeriodMs = 0;

    private final Semaphore semaphore;

    public CrptApi(int requestLimit, long periodLimit, TimeUnit periodTimeUnit) {
        this.limitMs = periodTimeUnit.toMillis(periodLimit);
        semaphore = new Semaphore(requestLimit);
    }

    public void createDocument(Document document) throws InterruptedException {
        if (startPeriodMs == 0)
            startPeriodMs = System.currentTimeMillis();

        semaphore.acquire();

        sendRequest(document);

        releaseSemaphore();
    }

    private void sendRequest(Document document) {
        try {
            HttpURLConnection urlConnection = (HttpURLConnection) new URL(URL).openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setDoOutput(true);
            writeBodyToStream(document, urlConnection.getOutputStream());
        } catch (Exception e) {
            System.out.printf("Connection error: %s", e.getMessage());
        }
    }

    private void writeBodyToStream(Document document, OutputStream stream) throws IOException {
        var json = mapper.writeValueAsString(document);
        var writer = new OutputStreamWriter(stream);
        writer.write(json);
        writer.flush();
    }

    public void releaseSemaphore() {
        if (semaphore.availablePermits() > 0) {
            semaphore.release();
        }

        if (startPeriodMs > System.currentTimeMillis()) {
            resetSemaphore();
        }

        if (semaphore.availablePermits() == 0) {
           new Thread(() -> {
                try {
                    Thread.sleep(System.currentTimeMillis() - startPeriodMs);
                    resetSemaphore();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
    }

    private void resetSemaphore() {
        semaphore.release();
        semaphore.drainPermits();
        startPeriodMs = System.currentTimeMillis();
    }

    @AllArgsConstructor
    @Getter
   public static class Document {
        private Description description;
        private String docId;
        private String docStatus;
        private String docType;
        private boolean importRequest;
        private String ownerInn;
        private String participantInn;
        private String producerInn;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private Date productionDate;
        private String productionType;
        private List<Product> products;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm a z")
        private Date regDate;
        private String regNumber;

        @AllArgsConstructor
        @Getter
        public static class Description {
            private String participantInn;
        }

        @AllArgsConstructor
        @Getter
        public static class Product {
            private String certificateDocument;
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
            private Date certificateDocumentDate;
            private String certificateDocumentNumber;
            private String ownerInn;
            private String producerInn;
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
            private Date productionDate;
            private String tnvedCode;
            private String uitCode;
            private String uituCode;
        }
    }
}

class CrptApiTest {

    private CrptApi crptApi  = new CrptApi(10, 1, TimeUnit.SECONDS);

    @Test
    void sendLimitedRequest() throws InterruptedException {
        var start = System.currentTimeMillis();
        var current = System.currentTimeMillis();
        var end = current + 50000;
        System.out.printf("Started at: %s\n", toLocalDateTime(current).toLocalTime());
        var countRequests = 0;
        while (current < end) {
            crptApi.createDocument(createRandomDocument());
            countRequests = countRequests + 1;
            current+=1000;
            System.out.printf("Sent request %d at: %s\n", countRequests, toLocalDateTime(current).toLocalTime());
        }
        System.out.println("All time: " + (end - start) / 10000 + " ms");
        System.out.printf("Ended at: %s", toLocalDateTime(System.currentTimeMillis()).toLocalTime());
        Assertions.assertEquals(50, countRequests);
    }

    private LocalDateTime toLocalDateTime(long ms) {
        var instance = Instant.ofEpochMilli(ms);
        return LocalDateTime.ofInstant(instance, ZoneId.systemDefault());
    }

    private CrptApi.Document createRandomDocument() {
        return new CrptApi.Document(new CrptApi.Document.Description("participantInn"), "docId", "docStatus",
                "docType", true, "ownerInn", "participantInn", "producerInn",
                Date.valueOf("2020-01-23"), "productionType",
                List.of(new CrptApi.Document.Product("certificateDocument",
                        Date.valueOf("2020-01-23"), "certificateDocumentNumber",
                        "ownerInn", "producerInn", Date.valueOf("2020-01-23"),
                        "tnvedCode", "uitCode", "uituCode"
                )), Date.valueOf("2020-01-23"), "regNumber");
    }
}

