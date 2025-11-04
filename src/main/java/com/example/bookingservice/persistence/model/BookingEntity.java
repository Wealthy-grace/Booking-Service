package com.example.bookingservice.persistence.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "bookings")
public class BookingEntity {

    @Id
    private String id;

    // ========== APPOINTMENT REFERENCE ==========
    @Indexed(unique = true)
    private String appointmentId;
    private String appointmentTitle;
    private LocalDateTime appointmentDateTime;

    // ========== PROPERTY DETAILS ==========
    @Indexed
    private Long propertyId;
    private String propertyTitle;
    private String propertyAddress;
    private boolean propertyIsRented;
    private String propertyImage;
    private String propertyImage2;
    private String propertyImage3;
    private String propertyImage4;
    private BigDecimal rentAmount;
    private String propertyDescription;

    // ========== REQUESTER (TENANT) DETAILS ==========
    @Indexed
    private Long requesterId;
    private String requesterUsername;
    private String requesterName;
    private String requesterEmail;
    private String requesterPhone;
    private String requesterFirstName;
    private String requesterLastName;
    private String requesterProfileImage;

    // ========== PROVIDER (LANDLORD) DETAILS ==========
    @Indexed
    private Long providerId;
    private String providerUsername;
    private String providerName;
    private String providerEmail;
    private String providerPhone;
    private String providerFirstName;
    private String providerLastName;
    private String providerProfileImage;

    // ========== BOOKING DETAILS ==========
    private LocalDateTime bookingDate;

    @Indexed
    private LocalDateTime moveInDate;
    private LocalDateTime moveOutDate;
    private Integer bookingDurationMonths;
    private String paymentId;

    // Financial details
    private BigDecimal totalAmount;
    private BigDecimal depositAmount;
    private BigDecimal monthlyRent;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;

    // ========== STATUS ==========
    @Indexed
    private BookingStatus status;
    private String notes;
    private PaymentStatus paymentStatus;
    private String cancellationReason;
    private LocalDateTime cancelledAt;

    // ========== PAYMENT RELATIONSHIP ==========
    @Transient
    private List<PaymentEntity> payments = new ArrayList<>();

    // Payment tracking
    private Integer totalPaymentsCount;
    private Integer completedPaymentsCount;
    private LocalDateTime lastPaymentDate;
    private LocalDateTime nextPaymentDueDate;
    private LocalDateTime paymentDeadline;
    private BigDecimal amount;
    private String currency;
    private PaymentType paymentType;    // DEPOSIT, FIRST_MONTH, MONTHLY_RENT
    private PaymentMethod paymentMethod;    // CASH, CREDIT_CARD

    // ========== CONTRACT DETAILS ==========
    private String contractUrl;
    private Boolean contractSigned;
    private LocalDateTime contractSignedDate;
    private String contractNumber;

    // ========== TIMESTAMPS ==========
    @Indexed
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ========== ADDITIONAL INFO ==========
    private Boolean emailNotificationSent;
    private String confirmationToken;
    private Boolean isActive;
    private Boolean requiresApproval;
    private String approvedBy;
    private LocalDateTime approvedAt;

    // ========== HELPER METHODS ==========

    public boolean isFullyPaid() {
        return paidAmount != null && totalAmount != null
                && paidAmount.compareTo(totalAmount) >= 0;
    }

    public boolean isPaymentOverdue() {
        return paymentDeadline != null
                && LocalDateTime.now().isAfter(paymentDeadline)
                && !isFullyPaid();
    }

    public boolean canMoveIn() {
        return status == BookingStatus.CONFIRMED
                && isFullyPaid()
                && contractSigned != null && contractSigned;
    }

    public void calculateRemainingAmount() {
        if (totalAmount != null && paidAmount != null) {
            this.remainingAmount = totalAmount.subtract(paidAmount);
        } else {
            this.remainingAmount = totalAmount;
        }
    }
}