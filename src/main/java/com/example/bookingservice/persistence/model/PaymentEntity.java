package com.example.bookingservice.persistence.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment Entity
 * Relationship: Many Payments belong to One Booking (Many-to-One)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "payments")
public class PaymentEntity {

    @Id
    private String id;

    // ========== RELATIONSHIP ==========
    @Indexed
    private String bookingId;           // FK to Booking (Many-to-One)

    // ========== PAYMENT DETAILS ==========
    private BigDecimal amount;
    private String currency;
    private PaymentType paymentType;    // DEPOSIT, FIRST_MONTH, MONTHLY_RENT
    private PaymentMethod paymentMethod; // CREDIT_CARD, BANK_TRANSFER, etc.

    @Indexed
    private PaymentStatus status;       // PENDING, COMPLETED, FAILED

    // ========== PAYER INFO ==========
    @Indexed
    private Long payerId;
    private String payerName;
    private String payerEmail;

    // ========== TRANSACTION ==========
    @Indexed(unique = true, sparse = true)
    private String transactionId;       // From payment gateway
    private String paymentReference;    // Internal reference
    private String paymentGateway;      // Stripe, PayPal, Mollie

    // ========== METADATA ==========
    private String description;
    private String receiptUrl;
    private String failureReason;

    // ========== TIMESTAMPS ==========
    @Indexed
    private LocalDateTime paymentDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}