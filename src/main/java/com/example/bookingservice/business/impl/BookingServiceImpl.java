package com.example.bookingservice.business.impl;

import com.example.bookingservice.business.interfaces.BookingService;
import com.example.bookingservice.business.mapper.BookingMapper;
import com.example.bookingservice.client.AppointmentServiceClient;
import com.example.bookingservice.domain.dto.AppointmentDto;
import com.example.bookingservice.domain.dto.BookingDto;
import com.example.bookingservice.domain.dto.PaymentDto;
import com.example.bookingservice.domain.request.CreateBookingRequest;
import com.example.bookingservice.domain.request.ProcessPaymentRequest;
import com.example.bookingservice.domain.response.AppointmentResponse;
import com.example.bookingservice.event.BookingEvent;
import com.example.bookingservice.exception.*;
import com.example.bookingservice.persistence.model.*;
import com.example.bookingservice.persistence.respository.BookingRepository;
import com.example.bookingservice.persistence.respository.PaymentRepository;
import com.example.bookingservice.producer.BookingEventProducer;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final BookingMapper bookingMapper;
    private final AppointmentServiceClient appointmentServiceClient;
    private final BookingEventProducer bookingEventProducer;


    // ========== CIRCUIT BREAKER PROTECTED METHOD ==========

//
//      Get appointment with Circuit Breaker protection
//     Uses String appointmentId for MongoDB compatibility
//
    @CircuitBreaker(name = "appointmentService", fallbackMethod = "getAppointmentFallback")
    @Retry(name = "appointmentService")
    public AppointmentResponse getAppointmentWithCircuitBreaker(String appointmentId) {
        log.debug(" [BOOKING] Calling Appointment Service for ID: {}", appointmentId);
        return appointmentServiceClient.getAppointmentById(appointmentId);
    }

//
//      Fallback when Appointment Service is unavailable
//      Must match signature: String appointmentId + Exception
//
    private AppointmentResponse getAppointmentFallback(String appointmentId, Exception ex) {
        log.error("⚠️ [BOOKING] APPOINTMENT SERVICE CIRCUIT BREAKER ACTIVATED for ID: {}. Reason: {}",
                appointmentId, ex.getMessage());

        // Create fallback AppointmentDto with minimal data
        AppointmentDto fallbackAppointment = new AppointmentDto();
        fallbackAppointment.setAppointmentId(appointmentId); // MongoDB String ID
        fallbackAppointment.setStatus(AppointmentStatus.PENDING);
        fallbackAppointment.setAppointmentTitle("Appointment Temporarily Unavailable");

        // Create fallback AppointmentResponse
        AppointmentResponse fallbackResponse = new AppointmentResponse();
        fallbackResponse.setSuccess(false);
        fallbackResponse.setMessage("Appointment Service temporarily unavailable. Using fallback data.");
        fallbackResponse.setAppointment(fallbackAppointment);

        log.warn(" [BOOKING] Using fallback appointment response");
        return fallbackResponse;
    }

   // BUSINESS LOGIC FOR BOOKINGSERVICEIMPL CODE
    @Override
//    @Transactional
    public BookingDto createBooking(CreateBookingRequest request) {
        log.info("Creating booking for appointment: {}", request.getAppointmentId());

        // Check if booking already exists for this appointment
        if (bookingRepository.existsByAppointmentId(request.getAppointmentId())) {
            throw new BookingAlreadyExistsException(
                    "Booking already exists for appointment: " + request.getAppointmentId()
            );
        }

        // Fetch ENRICHED appointment details from Appointment Service
        AppointmentResponse appointmentResponse;
        try {
            appointmentResponse = appointmentServiceClient.getAppointmentById(
                    request.getAppointmentId()
            );
        } catch (Exception e) {
            log.error("Failed to fetch appointment: {}", e.getMessage());
            throw new AppointmentNotFoundException(
                    "Appointment not found: " + request.getAppointmentId()
            );
        }

        if (appointmentResponse == null || !appointmentResponse.isSuccess()) {
            throw new AppointmentNotFoundException(
                    "Appointment not found: " + request.getAppointmentId()
            );
        }

        AppointmentDto appointment = appointmentResponse.getAppointment();

        // Validate appointment status
        if (!"CONFIRMED".equalsIgnoreCase(appointment.getStatus().toString()) &&
                !"PENDING".equalsIgnoreCase(appointment.getStatus().toString())) {
            throw new InvalidBookingException(
                    "Cannot book property. Appointment status is: " + appointment.getStatus()
            );
        }

        // Validate property availability
//        if (appointment.getPropertyIsRented() != null && appointment.getPropertyIsRented()) {
//            throw new InvalidBookingException(
//                    "Property is not available for booking: " + appointment.getPropertyTitle()
//            );
//        }

        // NEW CODE - NULL SAFE
        if (Boolean.TRUE.equals(appointment.getPropertyIsRented())) {
            throw new InvalidBookingException(
                    "Property is not available for booking: " + appointment.getPropertyTitle()
            );
        }

        // Validate dates
        if (request.getMoveInDate().isBefore(LocalDateTime.now())) {
            throw new InvalidBookingException("Move-in date cannot be in the past");
        }

        if (request.getMoveOutDate().isBefore(request.getMoveInDate())) {
            throw new InvalidBookingException("Move-out date must be after move-in date");
        }

        // Calculate booking details
        BigDecimal monthlyRent = appointment.getPropertyRentAmount();

        if (monthlyRent == null || monthlyRent.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidBookingException("Invalid rent amount for property");
        }

        BigDecimal depositAmount = monthlyRent;
        BigDecimal totalAmount = monthlyRent.multiply(
                BigDecimal.valueOf(request.getBookingDurationMonths())
        ).add(depositAmount);

        // Create booking
        BookingEntity booking = BookingEntity.builder()
                .appointmentId(appointment.getAppointmentId())
                .appointmentTitle(appointment.getAppointmentTitle())
                .appointmentDateTime(appointment.getAppointmentDateTime())
                .propertyId(appointment.getPropertyId())
                .propertyTitle(appointment.getPropertyTitle())
                .propertyAddress(appointment.getPropertyAddress())
                .propertyIsRented(appointment.getPropertyIsRented())
                .propertyImage(appointment.getPropertyImage())
                .propertyImage2(appointment.getPropertyImage2())
                .propertyImage3(appointment.getPropertyImage3())
                .propertyImage4(appointment.getPropertyImage4())
                .rentAmount(appointment.getPropertyRentAmount())
                .propertyDescription(appointment.getPropertyDescription())
                .requesterId(appointment.getRequesterId())
                .requesterUsername(appointment.getRequesterUsername())
                .requesterName(appointment.getRequesterName())
                .requesterEmail(appointment.getRequesterEmail())
                .requesterPhone(appointment.getRequesterPhone())
                .requesterProfileImage(appointment.getRequesterProfileImage())
                .providerId(appointment.getProviderId())
                .providerUsername(appointment.getProviderUsername())
                .providerName(appointment.getProviderName())
                .providerEmail(appointment.getProviderEmail())
                .providerPhone(appointment.getProviderPhone())
                .providerProfileImage(appointment.getProviderProfileImage())
                .bookingDate(LocalDateTime.now())
                .moveInDate(request.getMoveInDate())
                .moveOutDate(request.getMoveOutDate())
                .bookingDurationMonths(request.getBookingDurationMonths())
                .totalAmount(totalAmount)
                .depositAmount(depositAmount)
                .monthlyRent(monthlyRent)
                .status(BookingStatus.PENDING)
                .notes(request.getNotes())
                .paymentStatus(PaymentStatus.PENDING)
                .paymentDeadline(LocalDateTime.now().plusDays(7))
                .paidAmount(BigDecimal.ZERO)
                .remainingAmount(totalAmount)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .paymentType(PaymentType.DEPOSIT)
                .cancellationReason(null)
                .contractSigned(false)
                .emailNotificationSent(false)
                .confirmationToken(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        BookingEntity savedBooking = bookingRepository.save(booking);
        log.info("Booking created successfully: {}", savedBooking.getId());

        // Publish booking created event to RabbitMQ
        try {
            BookingEvent event = bookingMapper.toEvent(savedBooking);
            bookingEventProducer.publishBookingCreated(event);
            log.info("Published BOOKING_CREATED event for booking: {}", savedBooking.getId());
        } catch (Exception e) {
            log.error("Failed to publish booking created event: {}", e.getMessage(), e);
            // Don't fail the booking creation if event publishing fails
        }

        return bookingMapper.toDto(savedBooking);
    }

    @Override
    public BookingDto getBookingById(String bookingId) {
        log.info("Fetching booking: {}", bookingId);
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found: " + bookingId));
        return bookingMapper.toDto(booking);
    }

    @Override
    public List<BookingDto> getAllBookings() {
        log.info("Fetching all bookings");
        List<BookingEntity> bookings = bookingRepository.findAll();
        return bookingMapper.toDtoList(bookings);
    }

    @Override
    public List<BookingDto> getBookingsByAppointmentId(String appointmentId) {
        log.info("Fetching bookings for appointment: {}", appointmentId);
        List<BookingEntity> bookings = bookingRepository.findByAppointmentId(appointmentId);
        return bookingMapper.toDtoList(bookings);
    }

    @Override
    public BookingDto updatePropertyStatus(String propertyId, boolean propertyIsRented) {
        log.info("Updating property status for property: {}", propertyId);
        BookingEntity booking = bookingRepository.findById(propertyId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found: " + propertyId));
        booking.setPropertyIsRented(propertyIsRented);
        bookingRepository.save(booking);
        return bookingMapper.toDto(booking);
    }

    @Override
    public List<BookingDto> getBookingsByRequesterId(Long requesterId) {
        log.info("Fetching bookings for requester: {}", requesterId);
        List<BookingEntity> bookings = bookingRepository.findByRequesterId(requesterId);
        return bookingMapper.toDtoList(bookings);
    }

    @Override
    public List<BookingDto> getBookingsByProviderId(Long providerId) {
        log.info("Fetching bookings for provider: {}", providerId);
        List<BookingEntity> bookings = bookingRepository.findByProviderId(providerId);
        return bookingMapper.toDtoList(bookings);
    }

    @Override
    public List<BookingDto> getBookingsByPropertyId(Long propertyId) {
        log.info("Fetching bookings for property: {}", propertyId);
        List<BookingEntity> bookings = bookingRepository.findByPropertyId(propertyId);
        return bookingMapper.toDtoList(bookings);
    }

    @Override
    public List<BookingDto> getBookingsByStatus(BookingStatus status) {
        log.info("Fetching bookings with status: {}", status);
        List<BookingEntity> bookings = bookingRepository.findByStatus(status);
        return bookingMapper.toDtoList(bookings);
    }

    @Override
    @Transactional
    public BookingDto updateBookingStatus(String bookingId, BookingStatus status) {
        log.info("Updating booking {} status to {}", bookingId, status);
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found: " + bookingId));

        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(status);
        booking.setUpdatedAt(LocalDateTime.now());

        BookingEntity updatedBooking = bookingRepository.save(booking);

        // Publish status update event if status changed
        if (oldStatus != status) {
            try {
                BookingEvent event = bookingMapper.toEvent(updatedBooking);

                // Publish specific events based on the new status
                if (status == BookingStatus.CONFIRMED) {
                    bookingEventProducer.publishBookingConfirmed(event);
                } else if (status == BookingStatus.COMPLETED) {
                    bookingEventProducer.publishBookingCompleted(event);
                } else if (status == BookingStatus.EXPIRED) {
                    bookingEventProducer.publishBookingExpired(event);
                }

                log.info("Published booking status update event for booking: {}", updatedBooking.getId());
            } catch (Exception e) {
                log.error("Failed to publish booking status update event: {}", e.getMessage(), e);
            }
        }

        return bookingMapper.toDto(updatedBooking);
    }

    @Override
//    @Transactional
    public BookingDto cancelBooking(String bookingId, String reason) {
        log.info("Cancelling booking: {}", bookingId);
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found: " + bookingId));

        if (booking.getStatus() == BookingStatus.COMPLETED ||
                booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidBookingException(
                    "Cannot cancel booking with status: " + booking.getStatus()
            );
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        booking.setUpdatedAt(LocalDateTime.now());

        // Refund payment if already paid
        if (booking.getPaymentStatus() == PaymentStatus.COMPLETED) {
            PaymentEntity payment = paymentRepository.findByBookingId(bookingId)
                    .orElse(null);
            if (payment != null) {
                payment.setStatus(PaymentStatus.REFUNDED);
                payment.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(payment);
            }
            booking.setPaymentStatus(PaymentStatus.REFUNDED);
        }

        BookingEntity cancelledBooking = bookingRepository.save(booking);
        log.info("Booking cancelled successfully: {}", bookingId);

        // Publish booking cancelled event to RabbitMQ
        try {
            BookingEvent event = bookingMapper.toEvent(cancelledBooking);
            event.setCancelledAt(LocalDateTime.now());
            bookingEventProducer.publishBookingCancelled(event);
            log.info("Published BOOKING_CANCELLED event for booking: {}", cancelledBooking.getId());
        } catch (Exception e) {
            log.error("Failed to publish booking cancelled event: {}", e.getMessage(), e);
        }

        return bookingMapper.toDto(cancelledBooking);
    }

    @Override
//    @Transactional
    public void deleteBooking(String bookingId) {
        log.info("Deleting booking: {}", bookingId);
        if (!bookingRepository.existsById(bookingId)) {
            throw new BookingNotFoundException("Booking not found: " + bookingId);
        }
        bookingRepository.deleteById(bookingId);
        log.info("Booking deleted successfully: {}", bookingId);
    }

    @Override
//    @Transactional
    public PaymentDto processPayment(ProcessPaymentRequest request) {
        log.info("Processing payment for booking: {}", request.getBookingId());

        BookingEntity booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new BookingNotFoundException(
                        "Booking not found: " + request.getBookingId()
                ));

        if (booking.getPaymentStatus() == PaymentStatus.COMPLETED) {
            throw new PaymentException("Payment already completed for this booking");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new PaymentException("Cannot process payment for cancelled booking");
        }

        // Check payment deadline
        if (LocalDateTime.now().isAfter(booking.getPaymentDeadline())) {
            booking.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);
            throw new PaymentException("Payment deadline has passed");
        }

        // Create payment record
        PaymentEntity payment = PaymentEntity.builder()
                .bookingId(booking.getId())
                .amount(booking.getTotalAmount())
                .currency("EUR")
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.PROCESSING)
                .payerId(booking.getRequesterId())
                .payerName(booking.getRequesterName())
                .payerEmail(booking.getRequesterEmail())
                .transactionId(UUID.randomUUID().toString())
                .paymentReference(request.getPaymentReference())
                .paymentGateway("MOCK_GATEWAY")
                .description("Payment for booking: " + booking.getPropertyTitle())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Simulate payment processing
        try {
            Thread.sleep(1000); // Simulate processing time

            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setPaymentDate(LocalDateTime.now());
            payment.setReceiptUrl("https://receipts.example.com/" + payment.getTransactionId());

            booking.setPaymentId(payment.getId());
            booking.setPaymentStatus(PaymentStatus.COMPLETED);
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setPaidAmount(booking.getTotalAmount());
            booking.setRemainingAmount(BigDecimal.ZERO);
            booking.setUpdatedAt(LocalDateTime.now());

        } catch (Exception e) {
            log.error("Payment processing failed: {}", e.getMessage());
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(e.getMessage());
            booking.setPaymentStatus(PaymentStatus.FAILED);
        }

        PaymentEntity savedPayment = paymentRepository.save(payment);
        BookingEntity updatedBooking = bookingRepository.save(booking);

        // Publish payment completed event if payment was successful
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            try {
                BookingEvent event = bookingMapper.toEvent(updatedBooking);
                event.setTransactionId(savedPayment.getTransactionId());
                event.setPaymentReference(savedPayment.getPaymentReference());
                bookingEventProducer.publishBookingPaymentCompleted(event);
                log.info("Published BOOKING_PAYMENT_COMPLETED event for booking: {}", updatedBooking.getId());
            } catch (Exception e) {
                log.error("Failed to publish payment completed event: {}", e.getMessage(), e);
            }
        }

        log.info("Payment processed successfully: {}", savedPayment.getId());
        return bookingMapper.toDto(savedPayment);
    }

    @Override
    public PaymentDto getPaymentByBookingId(String bookingId) {
        log.info("Fetching payment for booking: {}", bookingId);
        PaymentEntity payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(
                        "Payment not found for booking: " + bookingId
                ));
        return bookingMapper.toDto(payment);
    }

    @Override
    public List<PaymentDto> getPaymentsByPayerId(Long payerId) {
        log.info("Fetching payments for payer: {}", payerId);
        List<PaymentEntity> payments = paymentRepository.findByPayerId(payerId);
        return bookingMapper.toPaymentDtoList(payments);
    }

    @Override
//    @Transactional
    public BookingDto confirmBooking(String confirmationToken) {
        log.info("Confirming booking with token: {}", confirmationToken);

        BookingEntity booking = bookingRepository.findByConfirmationToken(confirmationToken)
                .orElseThrow(() -> new BookingNotFoundException(
                        "Booking not found with confirmation token"
                ));

        if (booking.getPaymentStatus() != PaymentStatus.COMPLETED) {
            throw new InvalidBookingException(
                    "Cannot confirm booking. Payment not completed."
            );
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setUpdatedAt(LocalDateTime.now());

        BookingEntity confirmedBooking = bookingRepository.save(booking);
        log.info("Booking confirmed successfully: {}", confirmedBooking.getId());

        // Publish booking confirmed event
        try {
            BookingEvent event = bookingMapper.toEvent(confirmedBooking);
            bookingEventProducer.publishBookingConfirmed(event);
            log.info("Published BOOKING_CONFIRMED event for booking: {}", confirmedBooking.getId());
        } catch (Exception e) {
            log.error("Failed to publish booking confirmed event: {}", e.getMessage(), e);
        }

        return bookingMapper.toDto(confirmedBooking);
    }
}