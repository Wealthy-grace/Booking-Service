package com.example.bookingservice.domain.dto;

import com.example.bookingservice.persistence.model.PaymentMethod;
import com.example.bookingservice.persistence.model.PaymentStatus;
import com.example.bookingservice.persistence.model.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDto {

    private String id;

    // Relationship
    private String bookingId;

    // Payment details
    private BigDecimal amount;
    private String currency;
    private PaymentType paymentType;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;

    // Payer
    private Long payerId;
    private String payerName;
    private String payerEmail;

    // Transaction
    private String transactionId;
    private String paymentReference;
    private String paymentGateway;

    // Metadata
    private String description;
    private String receiptUrl;
    private String failureReason;

    // Timestamps
    private LocalDateTime paymentDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}