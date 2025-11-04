package com.example.bookingservice.domain.dto;



import com.example.bookingservice.persistence.model.AppointmentStatus;
import com.example.bookingservice.persistence.model.AppointmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentDto {

    // Basic appointment info
    private String appointmentId;
    private String appointmentTitle;
    private String description;
    private  LocalDateTime appointmentDateTime;
    private Integer durationMinutes;
    private AppointmentStatus status;
    private AppointmentType type;

    // IDs
    private Long propertyId;
    private Long requesterId;
    private Long providerId;

    // Location & timing
    private String location;
    private String notes;
    private LocalDateTime endDateTime;
    private Integer daysUntilAppointment;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Cancellation & recurring
    private String cancellationReason;
    private Boolean isRecurring;
    private String meetingLink;
    private Boolean reminderSent;
    private String confirmationToken;
    private Boolean canCancel;
    private Boolean canReschedule;

    // Requester (Tenant) details - ENRICHED by Appointment-Service
    private String requesterName;
    private String requesterUsername;
    private String requesterEmail;
    private String requesterPhone;
    private String requesterFirstName;
    private String requesterLastName;
    private String requesterProfileImage;

    // Provider (Landlord) details - ENRICHED by Appointment-Service
    private String providerName;
    private String providerUsername;
    private String providerEmail;
    private String providerPhone;
    private String providerFirstName;
    private String providerLastName;
    private String providerProfileImage;

    // Property details - ENRICHED by Appointment-Service
    private String propertyTitle;
    private String propertyAddress;
    private Boolean propertyIsRented;
    private String propertyImage;
    private String propertyImage2;
    private String propertyImage3;
    private String propertyImage4;
    private String propertyDescription;
    private BigDecimal propertyRentAmount;
}