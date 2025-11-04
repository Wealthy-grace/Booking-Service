package com.example.bookingservice.domain.request;
import com.example.bookingservice.persistence.model.PaymentMethod;
import com.example.bookingservice.persistence.model.PaymentStatus;
import com.example.bookingservice.persistence.model.PaymentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessPaymentRequest {

    @NotBlank(message = "Booking ID is required")
    private String bookingId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Payment type is required")
    private PaymentType paymentType;      // DEPOSIT, FIRST_MONTH, MONTHLY_RENT

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;  // CREDIT_CARD, BANK_TRANSFER, etc.

    private String currency;              // Default: EUR

    private String paymentGateway;        // Stripe, PayPal, Mollie

    private String paymentGatewayReference;

    private  String PaymentReference;

    private String description;
}
























