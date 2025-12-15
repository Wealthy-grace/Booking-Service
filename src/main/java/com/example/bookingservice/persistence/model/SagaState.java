package com.example.bookingservice.persistence.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SAGA State Entity
 * Tracks the progress of booking saga transactions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "saga_state")
public class SagaState {

    @Id
    private String id;

    /**
     * Type of saga (e.g., "BOOKING_SAGA", "APPOINTMENT_BOOKING_SAGA")
     */
    private String sagaType;

    /**
     * Related business entity IDs
     */
    private String bookingId;
    private String appointmentId;
    private Long propertyId;
    private Long requesterId;
    private Long providerId;

    /**
     * Current step in the saga
     * e.g., "APPOINTMENT_CONFIRMED", "BOOKING_CREATED", "PAYMENT_COMPLETED"
     */
    private String currentStep;

    /**
     * Status of the saga
     */
    private SagaStatus status;

    /**
     * Steps that have been successfully completed
     */
    @Builder.Default
    private List<String> completedSteps = new ArrayList<>();

    /**
     * Steps that failed and need compensation
     */
    @Builder.Default
    private List<String> failedSteps = new ArrayList<>();

    /**
     * Arbitrary data needed for saga execution
     */
    @Builder.Default
    private Map<String, Object> sagaData = new HashMap<>();

    /**
     * Error information if saga failed
     */
    private String errorMessage;
    private String errorStackTrace;

    /**
     * Timestamps
     */
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime lastUpdatedAt;

    /**
     * Retry tracking
     */
    @Builder.Default
    private Integer retryCount = 0;

    @Builder.Default
    private Integer maxRetries = 3;

    /**
     * Compensation tracking
     */
    private Boolean compensationRequired;
    private LocalDateTime compensationStartedAt;
    private LocalDateTime compensationCompletedAt;

    /**
     * Helper method to add completed step
     */
    public void addCompletedStep(String step) {
        if (completedSteps == null) {
            completedSteps = new ArrayList<>();
        }
        if (!completedSteps.contains(step)) {
            completedSteps.add(step);
        }
        this.lastUpdatedAt = LocalDateTime.now();
    }

    /**
     * Helper method to add failed step
     */
    public void addFailedStep(String step) {
        if (failedSteps == null) {
            failedSteps = new ArrayList<>();
        }
        if (!failedSteps.contains(step)) {
            failedSteps.add(step);
        }
        this.lastUpdatedAt = LocalDateTime.now();
    }

    /**
     * Helper method to update saga data
     */
    public void putSagaData(String key, Object value) {
        if (sagaData == null) {
            sagaData = new HashMap<>();
        }
        sagaData.put(key, value);
        this.lastUpdatedAt = LocalDateTime.now();
    }

    /**
     * Helper method to check if step was completed
     */
    public boolean isStepCompleted(String step) {
        return completedSteps != null && completedSteps.contains(step);
    }

    /**
     * Helper method to check if saga can be retried
     */
    public boolean canRetry() {
        return retryCount < maxRetries;
    }

    /**
     * Helper method to increment retry count
     */
    public void incrementRetryCount() {
        this.retryCount++;
        this.lastUpdatedAt = LocalDateTime.now();
    }
}