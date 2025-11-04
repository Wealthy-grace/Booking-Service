package com.example.bookingservice.persistence.model;

public enum PaymentMethod {

    CREDIT_CARD,
    DEBIT_CARD,
    BANK_TRANSFER,
    PAYPAL,
    STRIPE,
    IDEAL,             // Dutch payment method
    CASH,
    OTHER
}
