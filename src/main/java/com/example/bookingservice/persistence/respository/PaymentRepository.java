package com.example.bookingservice.persistence.respository;

import com.example.bookingservice.persistence.model.PaymentEntity;
import com.example.bookingservice.persistence.model.PaymentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends MongoRepository<PaymentEntity, String> {


    // Find by booking and status
    List<PaymentEntity> findByBookingIdAndStatus(String bookingId, PaymentStatus status);

    // Count payments for booking
    long countByBookingId(String bookingId);

    // Count completed payments for booking
    long countByBookingIdAndStatus(String bookingId, PaymentStatus status);


        // ✅ CORRECT: Returns Optional<Payment>
        Optional<PaymentEntity> findByBookingId(String bookingId);

        // For multiple payments if needed
        List<PaymentEntity> findAllByBookingId(String bookingId);

        List<PaymentEntity> findByPayerId(Long payerId);
        Optional<PaymentEntity> findByTransactionId(String transactionId);
        List<PaymentEntity> findByStatus(PaymentStatus status);
    }
