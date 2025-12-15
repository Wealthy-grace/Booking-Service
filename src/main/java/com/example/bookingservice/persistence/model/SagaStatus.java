package com.example.bookingservice.persistence.model;


/**
 * Enum representing the status of a SAGA transaction
 */
public enum SagaStatus {

    INITIATED,
    PROCESSING,

    /**
     * Saga completed successfully
     */
    COMPLETED,

    /**
     * Saga failed and compensation is required
     */
    FAILED,

    /**
     * Saga is being compensated (rollback in progress)
     */
    COMPENSATING,

    /**
     * Compensation completed successfully
     */
    COMPENSATED,

    CANCELLED,


    COMPENSATION_FAILED,

    /**
     * Saga is in retry state
     */
    RETRYING
}