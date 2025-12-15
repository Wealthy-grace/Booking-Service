package com.example.bookingservice.business.saga;

import com.example.bookingservice.business.interfaces.BookingService;
import com.example.bookingservice.domain.dto.BookingDto;
import com.example.bookingservice.domain.request.CreateBookingRequest;
import com.example.bookingservice.persistence.model.SagaState;
import com.example.bookingservice.persistence.model.SagaStatus;
import com.example.bookingservice.persistence.respository.SagaStateRepository;
import com.example.bookingservice.producer.BookingEventProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * SAGA Orchestrator for Booking Process
 * Manages the entire booking saga flow from appointment confirmation to booking completion
 */
@Slf4j
@Service
public class BookingSagaOrchestrator {

    private final SagaStateRepository sagaStateRepository;
    private final BookingEventProducer bookingEventProducer;
    private final SagaCompensationService compensationService;

    // ✅ FIXED: Use @Lazy to break circular dependency
    private final BookingService bookingService;

    private static final String SAGA_TYPE = "BOOKING_SAGA";
    private static final String STEP_APPOINTMENT_CONFIRMED = "APPOINTMENT_CONFIRMED";
    private static final String STEP_BOOKING_CREATED = "BOOKING_CREATED";
    private static final String STEP_PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
    private static final String STEP_PROPERTY_UPDATED = "PROPERTY_UPDATED";
    private static final String STEP_COMPLETED = "COMPLETED";

    // ✅ Constructor injection with @Lazy on BookingService
    @Autowired
    public BookingSagaOrchestrator(
            SagaStateRepository sagaStateRepository,
            BookingEventProducer bookingEventProducer,
            SagaCompensationService compensationService,
            @Lazy BookingService bookingService) {
        this.sagaStateRepository = sagaStateRepository;
        this.bookingEventProducer = bookingEventProducer;
        this.compensationService = compensationService;
        this.bookingService = bookingService;
    }

    /**
     * Start booking saga when appointment is confirmed
     * NOTE: This method is called AFTER booking is created, so we don't call createBooking here
     */
    @Transactional
    public SagaState startBookingSaga(String appointmentId, CreateBookingRequest bookingRequest) {
        log.info("🎬 Starting BOOKING SAGA for appointment: {}", appointmentId);

        try {
            // Check if saga already exists
            if (sagaStateRepository.existsByAppointmentId(appointmentId)) {
                log.warn("⚠️ SAGA already exists for appointment: {}", appointmentId);
                return sagaStateRepository.findByAppointmentId(appointmentId)
                        .orElseThrow(() -> new RuntimeException("Saga exists but cannot be retrieved"));
            }

            // Create saga state
            SagaState saga = SagaState.builder()
                    .id(UUID.randomUUID().toString())
                    .sagaType(SAGA_TYPE)
                    .appointmentId(appointmentId)
                    .currentStep(STEP_BOOKING_CREATED) // Booking already created at this point
                    .status(SagaStatus.PROCESSING)
                    .startedAt(LocalDateTime.now())
                    .lastUpdatedAt(LocalDateTime.now())
                    .retryCount(0)
                    .maxRetries(3)
                    .compensationRequired(false)
                    .build();

            // Store booking request data
            saga.putSagaData("bookingRequest", bookingRequest);
            saga.putSagaData("appointmentId", appointmentId);
            saga.addCompletedStep(STEP_APPOINTMENT_CONFIRMED);
            saga.addCompletedStep(STEP_BOOKING_CREATED);

            saga = sagaStateRepository.save(saga);
            log.info("✅ SAGA created with ID: {}", saga.getId());

            return saga;

        } catch (Exception e) {
            log.error("❌ Failed to start BOOKING SAGA: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to start booking saga", e);
        }
    }

    /**
     * Execute: Create Booking Step
     * NOTE: This is now only called for retry scenarios
     */
    @Transactional
    public void executeCreateBookingStep(SagaState saga, CreateBookingRequest bookingRequest) {
        log.info("📌 SAGA STEP: Creating/Retrying booking for appointment: {}", saga.getAppointmentId());

        try {
            // Update saga status
            saga.setStatus(SagaStatus.PROCESSING);
            saga.setCurrentStep(STEP_BOOKING_CREATED);
            sagaStateRepository.save(saga);

            // Check if booking already exists
            String appointmentId = saga.getAppointmentId();
            List<BookingDto> existingBookings = bookingService.getBookingsByAppointmentId(appointmentId);

            BookingDto booking;
            if (!existingBookings.isEmpty()) {
                log.info("ℹ️ Booking already exists for appointment: {}", appointmentId);
                booking = existingBookings.get(0);
            } else {
                // Create new booking (only in retry scenarios)
                booking = bookingService.createBooking(bookingRequest);
                log.info("✅ New booking created with ID: {}", booking.getId());
            }

            // Update saga with booking ID
            saga.setBookingId(booking.getId());
            saga.setPropertyId(booking.getPropertyId());
            saga.setRequesterId(booking.getRequesterId());
            saga.setProviderId(booking.getProviderId());
            saga.addCompletedStep(STEP_BOOKING_CREATED);
            saga.putSagaData("bookingId", booking.getId());

            sagaStateRepository.save(saga);

            log.info("✅ SAGA STEP COMPLETED: Booking ready with ID: {}", booking.getId());

        } catch (Exception e) {
            log.error("❌ SAGA STEP FAILED: {}", e.getMessage(), e);
            handleSagaFailure(saga, STEP_BOOKING_CREATED, e);
        }
    }

    /**
     * Handle payment completion
     */
    @Transactional
    public void handlePaymentCompleted(String bookingId, String transactionId) {
        log.info("💳 SAGA: Processing payment completion for booking: {}", bookingId);

        try {
            SagaState saga = sagaStateRepository.findByBookingId(bookingId)
                    .orElseThrow(() -> new RuntimeException("Saga not found for booking: " + bookingId));

            saga.setCurrentStep(STEP_PAYMENT_COMPLETED);
            saga.addCompletedStep(STEP_PAYMENT_COMPLETED);
            saga.putSagaData("transactionId", transactionId);
            saga.putSagaData("paymentCompletedAt", LocalDateTime.now());

            sagaStateRepository.save(saga);

            log.info("✅ SAGA: Payment completed for booking: {}", bookingId);

            // Mark saga as completed since payment is the final step
            completeSaga(saga);

        } catch (Exception e) {
            log.error("❌ SAGA: Payment completion handling failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Complete saga
     */
    @Transactional
    public void completeSaga(SagaState saga) {
        log.info("🎉 Completing SAGA: {}", saga.getId());

        saga.setStatus(SagaStatus.COMPLETED);
        saga.setCurrentStep(STEP_COMPLETED);
        saga.setCompletedAt(LocalDateTime.now());
        saga.setLastUpdatedAt(LocalDateTime.now());

        sagaStateRepository.save(saga);

        log.info("✅ SAGA COMPLETED successfully: {}", saga.getId());
    }

    /**
     * Handle saga failure and trigger compensation
     */
    @Transactional
    public void handleSagaFailure(SagaState saga, String failedStep, Exception exception) {
        log.error("❌ SAGA FAILED at step: {} - {}", failedStep, exception.getMessage());

        saga.setStatus(SagaStatus.FAILED);
        saga.setCurrentStep(failedStep);
        saga.addFailedStep(failedStep);
        saga.setErrorMessage(exception.getMessage());
        saga.setCompensationRequired(true);
        saga.setLastUpdatedAt(LocalDateTime.now());

        // Check if can retry
        if (saga.canRetry()) {
            saga.incrementRetryCount();
            saga.setStatus(SagaStatus.RETRYING);
            log.warn("🔄 SAGA will be retried. Attempt: {}/{}", saga.getRetryCount(), saga.getMaxRetries());
        }

        sagaStateRepository.save(saga);

        // Trigger compensation
        if (saga.getStatus() == SagaStatus.FAILED) {
            log.info("🔄 Triggering SAGA COMPENSATION for: {}", saga.getId());
            compensationService.compensateBookingSaga(saga);
        }
    }

    /**
     * Retry failed saga
     */
    @Transactional
    public void retrySaga(String sagaId) {
        log.info("🔄 Retrying SAGA: {}", sagaId);

        SagaState saga = sagaStateRepository.findById(sagaId)
                .orElseThrow(() -> new RuntimeException("Saga not found: " + sagaId));

        if (!saga.canRetry()) {
            log.error("❌ SAGA cannot be retried. Max retries reached: {}", saga.getMaxRetries());
            return;
        }

        String failedStep = saga.getCurrentStep();
        CreateBookingRequest bookingRequest = (CreateBookingRequest) saga.getSagaData().get("bookingRequest");

        if (STEP_BOOKING_CREATED.equals(failedStep)) {
            executeCreateBookingStep(saga, bookingRequest);
        }
    }

    /**
     * Get saga by booking ID
     */
    public SagaState getSagaByBookingId(String bookingId) {
        return sagaStateRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Saga not found for booking: " + bookingId));
    }

    /**
     * Get saga by appointment ID
     */
    public SagaState getSagaByAppointmentId(String appointmentId) {
        return sagaStateRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new RuntimeException("Saga not found for appointment: " + appointmentId));
    }

    /**
     * Cancel saga
     */
    @Transactional
    public void cancelSaga(String sagaId, String reason) {
        log.info("🚫 Cancelling SAGA: {} - Reason: {}", sagaId, reason);

        SagaState saga = sagaStateRepository.findById(sagaId)
                .orElseThrow(() -> new RuntimeException("Saga not found: " + sagaId));

        saga.setStatus(SagaStatus.CANCELLED);
        saga.setErrorMessage("Cancelled: " + reason);
        saga.setLastUpdatedAt(LocalDateTime.now());
        saga.setCompensationRequired(true);

        sagaStateRepository.save(saga);

        // Trigger compensation
        log.info("🔄 Triggering SAGA COMPENSATION for cancelled saga: {}", saga.getId());
        compensationService.compensateBookingSaga(saga);

        log.info("✅ SAGA cancelled successfully: {}", sagaId);
    }

    /**
     * Get all sagas by status
     */
    public java.util.List<SagaState> getSagasByStatus(SagaStatus status) {
        return sagaStateRepository.findByStatus(status);
    }

    /**
     * Get all failed sagas that can be retried
     */
    public java.util.List<SagaState> getRetryableSagas() {
        java.util.List<SagaState> allSagas = sagaStateRepository.findAll();
        return allSagas.stream()
                .filter(saga -> saga.getStatus() == SagaStatus.FAILED || saga.getStatus() == SagaStatus.RETRYING)
                .filter(SagaState::canRetry)
                .toList();
    }
}