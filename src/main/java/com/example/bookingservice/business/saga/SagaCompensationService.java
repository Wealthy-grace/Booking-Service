package com.example.bookingservice.business.saga;

import com.example.bookingservice.business.interfaces.BookingService;
import com.example.bookingservice.event.BookingEvent;
import com.example.bookingservice.persistence.model.SagaState;
import com.example.bookingservice.persistence.model.SagaStatus;
import com.example.bookingservice.persistence.respository.SagaStateRepository;
import com.example.bookingservice.producer.BookingEventProducer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


@Slf4j
@Service
@RequiredArgsConstructor
public class SagaCompensationService {

    private final SagaStateRepository sagaStateRepository;
    private final BookingService bookingService;
    private final BookingEventProducer bookingEventProducer;


    @Transactional
    public void compensateBookingSaga(SagaState saga) {
        log.info(" Starting COMPENSATION for SAGA: {}", saga.getId());

        try {
            saga.setStatus(SagaStatus.COMPENSATING);
            saga.setCompensationStartedAt(LocalDateTime.now());
            sagaStateRepository.save(saga);

            // Compensate completed steps in reverse order
            if (saga.isStepCompleted("BOOKING_CREATED")) {
                compensateBookingCreation(saga);
            }

            // Mark compensation as completed
            saga.setStatus(SagaStatus.COMPENSATED);
            saga.setCompensationCompletedAt(LocalDateTime.now());
            saga.setLastUpdatedAt(LocalDateTime.now());
            sagaStateRepository.save(saga);

            log.info(" COMPENSATION COMPLETED for SAGA: {}", saga.getId());

        } catch (Exception e) {
            log.error(" COMPENSATION FAILED for SAGA {}: {}", saga.getId(), e.getMessage(), e);

            saga.setStatus(SagaStatus.COMPENSATION_FAILED);
            saga.setErrorMessage("Compensation failed: " + e.getMessage());
            saga.setLastUpdatedAt(LocalDateTime.now());
            sagaStateRepository.save(saga);
        }
    }

    /**
     * Compensate booking creation (cancel the booking)
     */
    private void compensateBookingCreation(SagaState saga) {
        log.info(" COMPENSATING: Cancelling booking for SAGA: {}", saga.getId());

        try {
            String bookingId = saga.getBookingId();
            if (bookingId != null) {
                // Cancel the booking
                bookingService.cancelBooking(
                        bookingId,
                        "Booking cancelled due to saga failure: " + saga.getErrorMessage()
                );

                log.info(" COMPENSATION: Booking cancelled: {}", bookingId);

                // Publish compensation event
                publishCompensationEvent(saga, "BOOKING_CANCELLED_COMPENSATION");
            }

        } catch (Exception e) {
            log.error(" Failed to compensate booking creation: {}", e.getMessage(), e);
            throw new RuntimeException("Booking compensation failed", e);
        }
    }

    /**
     * Compensate payment (refund)
     */
    private void compensatePayment(SagaState saga) {
        log.info(" COMPENSATING: Refunding payment for SAGA: {}", saga.getId());

        try {
            String bookingId = saga.getBookingId();
            String transactionId = (String) saga.getSagaData().get("transactionId");

            if (bookingId != null && transactionId != null) {
                // Refund payment logic would go here
                log.info(" Payment refund initiated for transaction: {}", transactionId);

                publishCompensationEvent(saga, "PAYMENT_REFUNDED_COMPENSATION");
            }

        } catch (Exception e) {
            log.error(" Failed to compensate payment: {}", e.getMessage(), e);
            throw new RuntimeException("Payment compensation failed", e);
        }
    }

    /**
     * Publish compensation event
     */
    private void publishCompensationEvent(SagaState saga, String eventType) {
        try {
            BookingEvent event = BookingEvent.builder()
                    .eventType(eventType)
                    .eventTimestamp(LocalDateTime.now())
                    .bookingId(saga.getBookingId())
                    .appointmentId(saga.getAppointmentId())
                    .propertyId(saga.getPropertyId())
                    .requesterId(saga.getRequesterId())
                    .providerId(saga.getProviderId())
                    .cancellationReason(saga.getErrorMessage())
                    .build();

            bookingEventProducer.publishBookingEvent(event);

            log.info(" Compensation event published: {}", eventType);

        } catch (Exception e) {
            log.error(" Failed to publish compensation event: {}", e.getMessage(), e);
        }
    }

    /**
     * Retry compensation for failed compensations
     */
    @Transactional
    public void retryCompensation(String sagaId) {
        log.info("🔄 Retrying COMPENSATION for SAGA: {}", sagaId);

        SagaState saga = sagaStateRepository.findById(sagaId)
                .orElseThrow(() -> new RuntimeException("Saga not found: " + sagaId));

        if (saga.getStatus() == SagaStatus.COMPENSATION_FAILED) {
            compensateBookingSaga(saga);
        }
    }
}