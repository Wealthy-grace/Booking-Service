package com.example.bookingservice.domain.dto;


import com.example.bookingservice.persistence.model.BookingStatus;
import com.example.bookingservice.persistence.model.PaymentMethod;
import com.example.bookingservice.persistence.model.PaymentStatus;
import com.example.bookingservice.persistence.model.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDto {

    private String id;


    // Appointment
    private String appointmentId;
    private String appointmentTitle;
    private LocalDateTime appointmentDateTime;

    // Property
    private Long propertyId;
    private String propertyTitle;
    private String propertyAddress;
    private String propertyDescription;
    private String propertyImage;
    private  String propertyImage2;
    private String propertyImage3;
    private  String propertyImage4;
    private Boolean propertyIsRented;
    private BigDecimal rentAmount;

    // Tenant
    private Long requesterId;
    private String requesterUsername;
    private String requesterFirstName;
    private String requesterLastName;
    private String requesterName;
    private String requesterEmail;
    private String requesterPhone;

    // Landlord
    private Long providerId;
    private String providerName;
    private String providerEmail;
    private String providerPhone;

    // Booking dates
    private LocalDateTime bookingDate;
    private LocalDateTime moveInDate;
    private LocalDateTime moveOutDate;
    private Integer bookingDurationMonths;

    // Financial
    private BigDecimal totalAmount;
    private BigDecimal depositAmount;
    private BigDecimal monthlyRent;
    private BigDecimal paidAmount;

    // Status
    private BookingStatus status;
    private String notes;

    // Payment
    private LocalDateTime paymentDeadline;
    private List<PaymentDto> payments;  // Optional
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private PaymentType paymentType;
    // Contract
    private Boolean contractSigned;
    private String contractUrl;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}