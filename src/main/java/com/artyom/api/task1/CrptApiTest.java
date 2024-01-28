package com.artyom.api.task1;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.sql.Date;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class CrptApiTest {

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
        var crpt = new CrptApi(requestLimit, periodSecLimit, TimeUnit.SECONDS);
        var start = System.currentTimeMillis();
        System.out.println("Start at " + LocalTime.now());
        Stream.generate(() -> crpt.sendDocument(getDocument()))
                .limit(10).count();
        var end = System.currentTimeMillis();
        System.out.println("End at " + LocalTime.now() + " after " + (end - start) + " ms");
        var expectedTime = (count + 1) / 100 * 1000;
        Assertions.assertTrue(end - start > expectedTime);
    }

    private Document getDocument() {
        Document.Description description = new Document.Description("participantInn");
        List<Document.Product> products = List.of(new Document.Product(
                "certificateDocument", Date.valueOf("2020-01-23"), "certificateDocumentNumber",
                "ownerInn", "producerInn", Date.valueOf("2020-01-23"), "tnvedCode", "uitCode", "uituCode"
        ));
        return new Document(
                description, "docId", "docStatus", "docType",
                true, "ownerInn", "participantInn", "producerInn",
                Date.valueOf("2020-01-23"), "productionType", products,
                Date.valueOf("2020-01-23"), "regNumber"
        );
    }
}
