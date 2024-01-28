package com.artyom.api.task1;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.sql.Date;

import java.util.List;

@AllArgsConstructor
@Getter
public class Document {
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
