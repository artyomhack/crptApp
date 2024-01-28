package com.artyom.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Date;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;


@Getter
public class CrptApi {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private static final Logger logger = LoggerFactory.getLogger(CrptApi.class);
    private final Long limitMs;
    private final TimeUnit periodTimeUnit;
    private final int requestLimit;
    private final Semaphore semaphore;
    private ScheduledExecutorService scheduledExecutor;
    private AtomicLong counterRequest = new AtomicLong(0);
    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.periodTimeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.limitMs = timeUnit.toMillis(requestLimit);
        semaphore = new Semaphore(requestLimit);
        scheduledExecutor = new ScheduledThreadPoolExecutor(requestLimit);
    }

    public void createDocument(Document document) {
        scheduledExecutor.scheduleWithFixedDelay(() -> {
            try {
                semaphore.acquire();
                sendRequest(document);
                counterRequest.incrementAndGet();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                semaphore.release();
            }
        }, 0,limitMs, periodTimeUnit);
    }

    public void sendRequest(Document document) {
        try {
            HttpURLConnection urlConnection = (HttpURLConnection) new URL(URL).openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setDoOutput(true);
            writeBodyToStream(document, urlConnection.getOutputStream());
            logger.info(() -> "Request sent: " + document.docId);
        } catch (Exception e) {
            logger.info(() -> "Error response: " + document.docId);
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
            "ownerInn", "producerInn", Date.valueOf("2020-01-23"), "tnvedCode", "uitCode", "uituCode"
    ));
    private final CrptApi.Document document = new CrptApi.Document(
            description, "docId", "docStatus", "docType",
            true, "ownerInn", "participantInn", "producerInn",
            Date.valueOf("2020-01-23"), "productionType", products,
            Date.valueOf("2020-01-23"), "regNumber"
    );

    private CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 5);

    @Test
    void sendLimitedRequest() throws InterruptedException, ExecutionException {
        var start = System.currentTimeMillis();
        var end = start + 5000;
        System.out.printf("Started at: %s\n", LocalTime.now());
        long countRequests = 0;
        while (end > System.currentTimeMillis()) {
            crptApi.createDocument(document);
        }
        System.out.println("All time: " + (end - start) / 1000 + " s");
        System.out.printf("Ended at: %s", LocalTime.now());
        System.out.println(countRequests);
        Assertions.assertEquals(25, crptApi.getCounterRequest().get());
    }
}

