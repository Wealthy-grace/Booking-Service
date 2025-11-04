package com.example.bookingservice.domain.response;

import com.example.bookingservice.domain.dto.PaymentDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Payment Response
 * Response wrapper for payment operations (single or multiple)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Boolean success;
    private String message;

    // For single payment
    private PaymentDto payment;

    // For multiple payments
    private List<PaymentDto> payments;
    private Integer count;

    private String errorCode;
    private LocalDateTime timestamp;

    /**
     * Create success response for single payment
     */
    public static PaymentResponse success(String message, PaymentDto payment) {
        return PaymentResponse.builder()
                .success(true)
                .message(message)
                .payment(payment)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create success response for payment list
     */
    public static PaymentResponse successList(String message, List<PaymentDto> payments) {
        return PaymentResponse.builder()
                .success(true)
                .message(message)
                .payments(payments)
                .count(payments != null ? payments.size() : 0)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create error response
     */
    public static PaymentResponse error(String message, String errorCode) {
        return PaymentResponse.builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create error response without error code
     */
    public static PaymentResponse error(String message) {
        return error(message, null);
    }
}