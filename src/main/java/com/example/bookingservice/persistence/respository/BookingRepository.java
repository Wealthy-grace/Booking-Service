package com.example.bookingservice.persistence.respository;

import com.example.bookingservice.persistence.model.BookingEntity;
import com.example.bookingservice.persistence.model.BookingStatus;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends MongoRepository<BookingEntity, String> {

    // Find by appointment
    List<BookingEntity> findByAppointmentId(String appointmentId);

     Optional<BookingEntity>  findByConfirmationToken(String confirmationToken);

    // Find by appointment
    boolean existsByAppointmentId(String appointmentId);

    // Find by tenant (requester)
    List<BookingEntity> findByRequesterId(Long requesterId);

    // Find by landlord (provider)
    List<BookingEntity> findByProviderId(Long providerId);

    // Find by property
    List<BookingEntity> findByPropertyId(Long propertyId);

    // Find by status
    List<BookingEntity> findByStatus(BookingStatus status);

    // Find by move-in date range
    List<BookingEntity> findByMoveInDateBetween(LocalDateTime start, LocalDateTime end);

    // Find overdue bookings
    List<BookingEntity> findByPaymentDeadlineBeforeAndStatusNot(
            LocalDateTime deadline, BookingStatus status
    );
}
