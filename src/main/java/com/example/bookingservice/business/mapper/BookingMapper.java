
// TODO: Add more mappings

//package com.example.bookingservice.business.mapper;
//
//import com.example.bookingservice.domain.dto.BookingDto;
//import com.example.bookingservice.domain.dto.PaymentDto;
//import com.example.bookingservice.persistence.model.BookingEntity;
//import com.example.bookingservice.persistence.model.PaymentEntity;
//import org.mapstruct.*;
//
//import java.util.List;
//
//@Mapper(
//        componentModel = "spring",
//        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
//)
//public interface BookingMapper {
//
//    // Booking mappings - MapStruct will auto-map all matching fields
//    BookingDto toDto(BookingEntity booking);
//
//    BookingEntity toEntity(BookingDto bookingDto);
//
//    List<BookingDto> toDtoList(List<BookingEntity> bookings);
//
//    void updateBookingFromDto(BookingDto dto, @MappingTarget BookingEntity booking);
//
//    // Payment mappings
//    PaymentDto toDto(PaymentEntity payment);
//
//    PaymentEntity toEntity(PaymentDto paymentDto);
//
//    List<PaymentDto> toPaymentDtoList(List<PaymentEntity> payments);
//}

// TODO: Newer version of the above mapper

package com.example.bookingservice.business.mapper;

import com.example.bookingservice.domain.dto.BookingDto;
import com.example.bookingservice.domain.dto.PaymentDto;
import com.example.bookingservice.event.BookingEvent;
import com.example.bookingservice.persistence.model.BookingEntity;
import com.example.bookingservice.persistence.model.PaymentEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;


@Component
public class BookingMapper {

    public BookingDto toDto(BookingEntity entity) {
        if (entity == null) {
            return null;
        }

        return BookingDto.builder()
                .id(entity.getId())
                .appointmentId(entity.getAppointmentId())
                .appointmentTitle(entity.getAppointmentTitle())
                .appointmentDateTime(entity.getAppointmentDateTime())
                .propertyId(entity.getPropertyId())
                .propertyTitle(entity.getPropertyTitle())
                .propertyAddress(entity.getPropertyAddress())
                .propertyIsRented(entity.isPropertyIsRented())
                .propertyImage(entity.getPropertyImage())
                .propertyImage2(entity.getPropertyImage2())
                .propertyImage3(entity.getPropertyImage3())
                .propertyImage4(entity.getPropertyImage4())
                .rentAmount(entity.getRentAmount())
                .propertyDescription(entity.getPropertyDescription())
                .requesterId(entity.getRequesterId())
                .requesterUsername(entity.getRequesterUsername())
                .requesterName(entity.getRequesterName())
                .requesterEmail(entity.getRequesterEmail())
                .requesterPhone(entity.getRequesterPhone())
                .requesterFirstName(entity.getRequesterFirstName())
                .requesterLastName(entity.getRequesterLastName())
                //.requesterProfileImage(entity.getRequesterProfileImage())
                .providerId(entity.getProviderId())
                //.providerUsername(entity.getProviderUsername())
                .providerName(entity.getProviderName())
                .providerEmail(entity.getProviderEmail())
                .providerPhone(entity.getProviderPhone())
                //.providerFirstName(entity.getProviderFirstName())
                // .providerLastName(entity.getProviderLastName())
                // .providerProfileImage(entity.getProviderProfileImage())
                .bookingDate(entity.getBookingDate())
                .moveInDate(entity.getMoveInDate())
                .moveOutDate(entity.getMoveOutDate())
                .bookingDurationMonths(entity.getBookingDurationMonths())
                //.paymentId(entity.getPaymentId())
                .totalAmount(entity.getTotalAmount())
                .depositAmount(entity.getDepositAmount())
                .monthlyRent(entity.getMonthlyRent())
                .paidAmount(entity.getPaidAmount())
                //.remainingAmount(entity.getRemainingAmount())
                .status(entity.getStatus())
                .notes(entity.getNotes())
                // .paymentStatus(entity.getPaymentStatus())
                //.cancellationReason(entity.getCancellationReason())
                //.cancelledAt(entity.getCancelledAt())
                //.totalPaymentsCount(entity.getTotalPaymentsCount())
                // .completedPaymentsCount(entity.getCompletedPaymentsCount())
                // .lastPaymentDate(entity.getLastPaymentDate())
                // .nextPaymentDueDate(entity.getNextPaymentDueDate())
                .paymentDeadline(entity.getPaymentDeadline())
                .contractUrl(entity.getContractUrl())
                .contractSigned(entity.getContractSigned())
                .paymentStatus(entity.getPaymentStatus())
                .paymentType(entity.getPaymentType())
                .paymentMethod(entity.getPaymentMethod())

                //.contractSignedDate(entity.getContractSignedDate())
                ///.contractNumber(entity.getContractNumber())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                // .emailNotificationSent(entity.getEmailNotificationSent())
                //.confirmationToken(entity.getConfirmationToken())
                //.isActive(entity.getIsActive())
                //.requiresApproval(entity.getRequiresApproval())
                //.approvedBy(entity.getApprovedBy())
                // .approvedAt(entity.getApprovedAt())
                .payments(null) // Will be populated separately if needed
                .build();
    }

    public BookingEntity toEntity(BookingDto dto) {
        if (dto == null) {
            return null;
        }

        return BookingEntity.builder()
                .id(dto.getId())
                .appointmentId(dto.getAppointmentId())
                .appointmentTitle(dto.getAppointmentTitle())
                .appointmentDateTime(dto.getAppointmentDateTime())
                .propertyId(dto.getPropertyId())
                .propertyTitle(dto.getPropertyTitle())
                .propertyAddress(dto.getPropertyAddress())
                .propertyIsRented(dto.getPropertyIsRented() != null && dto.getPropertyIsRented())
                .propertyImage(dto.getPropertyImage())
                .propertyImage2(dto.getPropertyImage2())
                .propertyImage3(dto.getPropertyImage3())
                .propertyImage4(dto.getPropertyImage4())
                .rentAmount(dto.getRentAmount())

                .requesterId(dto.getRequesterId())
                .requesterName(dto.getRequesterName())
                .requesterEmail(dto.getRequesterEmail())
                .requesterPhone(dto.getRequesterPhone())
                .requesterProfileImage(dto.getPropertyImage())

                .providerId(dto.getProviderId())

                .providerName(dto.getProviderName())
                .providerEmail(dto.getProviderEmail())
                .providerPhone(dto.getProviderPhone())
                .bookingDate(dto.getBookingDate())
                .moveInDate(dto.getMoveInDate())
                .moveOutDate(dto.getMoveOutDate())
                .bookingDurationMonths(dto.getBookingDurationMonths())

                .totalAmount(dto.getTotalAmount())
                .depositAmount(dto.getDepositAmount())
                .monthlyRent(dto.getMonthlyRent())
                .paidAmount(dto.getPaidAmount())
                .status(dto.getStatus())
                .notes(dto.getNotes())

                .paymentDeadline(dto.getPaymentDeadline())
                .paymentMethod(dto.getPaymentMethod())
                .paymentType(dto.getPaymentType())
                .contractUrl(dto.getContractUrl())
                .contractSigned(dto.getContractSigned())

                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }

    public List<BookingDto> toDtoList(List<BookingEntity> bookings) {
        if (bookings == null) {
            return null;
        }
        return bookings.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public void updateBookingFromDto(BookingDto dto, BookingEntity entity) {
        if (dto == null || entity == null) {
            return;
        }

        if (dto.getAppointmentTitle() != null) entity.setAppointmentTitle(dto.getAppointmentTitle());
        if (dto.getPropertyTitle() != null) entity.setPropertyTitle(dto.getPropertyTitle());
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
        if (dto.getNotes() != null) entity.setNotes(dto.getNotes());
        if (dto.getPaymentStatus() != null) entity.setPaymentStatus(dto.getPaymentStatus());

        entity.setUpdatedAt(java.time.LocalDateTime.now());
    }

    public PaymentDto toDto(PaymentEntity entity) {
        if (entity == null) {
            return null;
        }

        return PaymentDto.builder()
                .id(entity.getId())
                .bookingId(entity.getBookingId())
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .paymentType(entity.getPaymentType())
                .paymentMethod(entity.getPaymentMethod())
                .status(entity.getStatus())
                .payerId(entity.getPayerId())
                .payerName(entity.getPayerName())
                .payerEmail(entity.getPayerEmail())
                .transactionId(entity.getTransactionId())
                .paymentReference(entity.getPaymentReference())
                .paymentGateway(entity.getPaymentGateway())
                .description(entity.getDescription())
                .receiptUrl(entity.getReceiptUrl())
                .failureReason(entity.getFailureReason())
                .paymentDate(entity.getPaymentDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public PaymentEntity toEntity(PaymentDto dto) {
        if (dto == null) {
            return null;
        }

        return PaymentEntity.builder()
                .id(dto.getId())
                .bookingId(dto.getBookingId())
                .amount(dto.getAmount())
                .currency(dto.getCurrency())
                .paymentType(dto.getPaymentType())
                .paymentMethod(dto.getPaymentMethod())
                .status(dto.getStatus())
                .payerId(dto.getPayerId())
                .payerName(dto.getPayerName())
                .payerEmail(dto.getPayerEmail())
                .transactionId(dto.getTransactionId())
                .paymentReference(dto.getPaymentReference())
                .paymentGateway(dto.getPaymentGateway())
                .description(dto.getDescription())
                .receiptUrl(dto.getReceiptUrl())
                .failureReason(dto.getFailureReason())
                .paymentDate(dto.getPaymentDate())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }

    public List<PaymentDto> toPaymentDtoList(List<PaymentEntity> payments) {
        if (payments == null) {
            return null;
        }
        return payments.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    public BookingEvent toEvent(BookingEntity entity) {
        if (entity == null) {
            return null;
        }

        return BookingEvent.builder()
                // Booking core details
                .bookingId(entity.getId())
                .appointmentId(entity.getAppointmentId())
                .bookingDate(entity.getBookingDate())
                .moveInDate(entity.getMoveInDate())
                .moveOutDate(entity.getMoveOutDate())
                .bookingDurationMonths(entity.getBookingDurationMonths())
                .status(entity.getStatus() != null ? entity.getStatus().toString() : null)
                .notes(entity.getNotes())

                // Financial details
                .totalAmount(entity.getTotalAmount())
                .depositAmount(entity.getDepositAmount())
                .monthlyRent(entity.getMonthlyRent())
                .paidAmount(entity.getPaidAmount())
                .remainingAmount(entity.getRemainingAmount())
                .paymentStatus(entity.getPaymentStatus() != null ? entity.getPaymentStatus().toString() : null)
                .paymentMethod(entity.getPaymentMethod() != null ? entity.getPaymentMethod().toString() : null)
                .paymentType(entity.getPaymentType() != null ? entity.getPaymentType().toString() : null)
                .paymentDeadline(entity.getPaymentDeadline())
                .paymentId(entity.getPaymentId())

                // Property details
                .propertyId(entity.getPropertyId())
                .propertyTitle(entity.getPropertyTitle())
                .propertyAddress(entity.getPropertyAddress())
                .propertyImage(entity.getPropertyImage())

                // Tenant details (Requester)
                .requesterId(entity.getRequesterId())
                .requesterUsername(entity.getRequesterUsername())
                .requesterName(entity.getRequesterName())
                .requesterEmail(entity.getRequesterEmail())
                .requesterPhone(entity.getRequesterPhone())

                // Landlord details (Provider)
                .providerId(entity.getProviderId())
                .providerUsername(entity.getProviderUsername())
                .providerName(entity.getProviderName())
                .providerEmail(entity.getProviderEmail())
                .providerPhone(entity.getProviderPhone())

                // Contract and cancellation details
                .cancellationReason(entity.getCancellationReason())
                .contractSigned(entity.getContractSigned())
                .contractUrl(entity.getContractUrl())

                .build();
}
}
