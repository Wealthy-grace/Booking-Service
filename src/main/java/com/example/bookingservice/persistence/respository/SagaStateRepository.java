package com.example.bookingservice.persistence.respository;
import com.example.bookingservice.persistence.model.SagaState;
import com.example.bookingservice.persistence.model.SagaStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for SAGA State persistence
 */
@Repository
public interface SagaStateRepository extends MongoRepository<SagaState, String> {

    /**
     * Find saga by booking ID
     */
    Optional<SagaState> findByBookingId(String bookingId);

    /**
     * Find saga by appointment ID
     */
    Optional<SagaState> findByAppointmentId(String appointmentId);

    /**
     * Find sagas by status
     */
    List<SagaState> findByStatus(SagaStatus status);

    /**
     * Find sagas by status and type
     */
    List<SagaState> findByStatusAndSagaType(SagaStatus status, String sagaType);

    /**
     * Find failed sagas that can be retried
     */
    List<SagaState> findByStatusAndRetryCountLessThan(SagaStatus status, Integer maxRetries);

    /**
     * Find sagas that need compensation
     */
    List<SagaState> findByCompensationRequiredAndStatus(Boolean compensationRequired, SagaStatus status);

    /**
     * Find stale sagas (processing for too long)
     */
    List<SagaState> findByStatusAndLastUpdatedAtBefore(SagaStatus status, LocalDateTime before);

    /**
     * Check if saga exists for booking
     */
    boolean existsByBookingId(String bookingId);

    /**
     * Check if saga exists for appointment
     */
    boolean existsByAppointmentId(String appointmentId);
}