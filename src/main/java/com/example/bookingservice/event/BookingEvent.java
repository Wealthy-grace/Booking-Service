package com.example.bookingservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event object for booking-related messages
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    // Event metadata
    private String eventType;
    private LocalDateTime eventTimestamp;
    private String eventId;

    // Booking core details
    private String bookingId;
    private String appointmentId;
    private LocalDateTime bookingDate;
    private LocalDateTime moveInDate;
    private LocalDateTime moveOutDate;
    private Integer bookingDurationMonths;
    private String status;
    private String notes;

    // Financial details
    private BigDecimal totalAmount;
    private BigDecimal depositAmount;
    private BigDecimal monthlyRent;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private String paymentStatus;
    private String paymentMethod;
    private String paymentType;
    private LocalDateTime paymentDeadline;

    // Property details
    private Long propertyId;
    private String propertyTitle;
    private String propertyAddress;
    private String propertyImage;

    // Tenant details (Requester)
    private Long requesterId;
    private String requesterUsername;
    private String requesterName;
    private String requesterEmail;
    private String requesterPhone;

    // Landlord details (Provider)
    private Long providerId;
    private String providerUsername;
    private String providerName;
    private String providerEmail;
    private String providerPhone;

    // Cancellation details
    private String cancellationReason;
    private LocalDateTime cancelledAt;

    // Contract details
    private Boolean contractSigned;
    private String contractUrl;

    // Payment details
    private String paymentId;
    private String transactionId;
    private String paymentReference;
}