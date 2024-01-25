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
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


@Getter
public class CrptApi {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final Long limitMs;
    private final TimeUnit periodTimeUnit;
    private final int requestLimit;
    private final AtomicLong counts = new AtomicLong(0);
    private final AtomicLong countRequest = new AtomicLong(0);
    private final Semaphore semaphore;
    private final ScheduledExecutorService scheduledExecutor;

    public CrptApi(int requestLimit, long periodLimit, TimeUnit periodTimeUnit) {
        this.requestLimit = requestLimit;
        this.periodTimeUnit = periodTimeUnit;
        this.limitMs = periodTimeUnit.toMillis(periodLimit);
        semaphore = new Semaphore(requestLimit);
        scheduledExecutor = new ScheduledThreadPoolExecutor(requestLimit);
    }

    public AtomicLong createDocument(Document document) {
        scheduledExecutor.scheduleWithFixedDelay(() -> {
            try {
                semaphore.acquire();
                sendRequest(document);
                countRequest.getAndIncrement();
                counts.getAndAdd(countRequest.get());
                System.out.println("Atomic count: " +  counts.get());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                if (countRequest.get() > requestLimit) {
                    countRequest.set(0);
                    semaphore.drainPermits();
                } else
                    semaphore. release();
            }
        }, 0, limitMs, periodTimeUnit);
        return counts;
    }

    public void sendRequest(Document document) {
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

    private final CrptApi.Document.Description description = new CrptApi.Document.Description("participantInn");
    private final List<CrptApi.Document.Product> products = List.of(new CrptApi.Document.Product(
            "certificateDocument", Date.valueOf("2020-01-23"), "certificateDocumentNumber",
            "ownerInn", "producerInn", Date.valueOf("2020-01-23"), "tnvedCode","uitCode", "uituCode"
    ));
    private final CrptApi.Document document =  new CrptApi.Document(
                        description, "docId", "docStatus", "docType",
            true, "ownerInn", "participantInn", "producerInn",
            Date.valueOf("2020-01-23"), "productionType", products,
                  Date.valueOf("2020-01-23"), "regNumber"
    );

    private CrptApi crptApi  = new CrptApi(5, 1, TimeUnit.SECONDS);

    @Test
    void sendLimitedRequest() throws InterruptedException {
        var start = System.currentTimeMillis();
        var end = start + 5000;
        System.out.printf("Started at: %s\n", LocalTime.now());
        long countRequests = 0;
        while (end > System.currentTimeMillis()) {
            countRequests = crptApi.createDocument(document).get();
        }
        System.out.println("All time: " + (end - start) / 1000 + " s");
        System.out.printf("Ended at: %s", LocalTime.now());
        System.out.println(countRequests);
        Assertions.assertEquals(25, countRequests);
    }
}

