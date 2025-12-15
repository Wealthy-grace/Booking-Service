package com.example.bookingservice.business.impl;

import com.example.bookingservice.business.interfaces.BookingService;
import com.example.bookingservice.business.mapper.BookingMapper;
import com.example.bookingservice.business.saga.BookingSagaOrchestrator;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class BookingServiceImpl implements BookingService {

    // REQUIRED dependencies - injected via constructor
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final BookingMapper bookingMapper;
    private final AppointmentServiceClient appointmentServiceClient;

    // OPTIONAL dependency - RabbitMQ producer (may be null if RabbitMQ is disabled)
    @Autowired(required = false)
    private BookingEventProducer bookingEventProducer;

    // ✅ FIXED: ADD @Lazy to break circular dependency
    @Autowired(required = false)
    @Lazy
    private BookingSagaOrchestrator sagaOrchestrator;

    // Constructor with only required dependencies
    public BookingServiceImpl(
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            BookingMapper bookingMapper,
            AppointmentServiceClient appointmentServiceClient) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.bookingMapper = bookingMapper;
        this.appointmentServiceClient = appointmentServiceClient;
    }

    /**
     * Helper method to safely publish events to RabbitMQ
     * Only publishes if RabbitMQ is enabled and producer is available
     */
    private void publishEventSafely(Runnable eventPublisher, String eventDescription) {
        if (bookingEventProducer != null) {
            try {
                eventPublisher.run();
                log.info("📤 Published {} event", eventDescription);
            } catch (Exception e) {
                log.error("❌ Failed to publish {} event: {}", eventDescription, e.getMessage(), e);
                // Don't fail the operation if event publishing fails
            }
        } else {
            log.debug("ℹ️ RabbitMQ disabled - skipping {} event", eventDescription);
        }
    }

    // ========== CIRCUIT BREAKER PROTECTED METHOD ==========

    @CircuitBreaker(name = "appointmentService", fallbackMethod = "getAppointmentFallback")
    @Retry(name = "appointmentService")
    public AppointmentResponse getAppointmentWithCircuitBreaker(String appointmentId) {
        log.debug("🔍 [BOOKING] Calling Appointment Service for ID: {}", appointmentId);
        return appointmentServiceClient.getAppointmentById(appointmentId);
    }

    private AppointmentResponse getAppointmentFallback(String appointmentId, Exception ex) {
        log.error("⚠️ [BOOKING] APPOINTMENT SERVICE CIRCUIT BREAKER ACTIVATED for ID: {}. Reason: {}",
                appointmentId, ex.getMessage());

        AppointmentDto fallbackAppointment = new AppointmentDto();
        fallbackAppointment.setAppointmentId(appointmentId);
        fallbackAppointment.setStatus(AppointmentStatus.PENDING);
        fallbackAppointment.setAppointmentTitle("Appointment Temporarily Unavailable");

        AppointmentResponse fallbackResponse = new AppointmentResponse();
        fallbackResponse.setSuccess(false);
        fallbackResponse.setMessage("Appointment Service temporarily unavailable. Using fallback data.");
        fallbackResponse.setAppointment(fallbackAppointment);

        log.warn("⚡ [BOOKING] Using fallback appointment response");
        return fallbackResponse;
    }

    // ========== BOOKING CRUD OPERATIONS ==========

    @Override
    public BookingDto createBooking(CreateBookingRequest request) {
        log.info("📌 Creating booking for appointment ID: {}", request.getAppointmentId());

        // 1️⃣ Check if booking already exists
        if (bookingRepository.existsByAppointmentId(request.getAppointmentId())) {
            log.warn("⚠️ Booking already exists for appointment ID: {}", request.getAppointmentId());
            throw new BookingAlreadyExistsException(
                    "Booking already exists for appointment ID: " + request.getAppointmentId()
            );
        }

        // 2️⃣ Fetch appointment details
        AppointmentResponse appointmentResponse;
        try {
            appointmentResponse = appointmentServiceClient.getAppointmentById(request.getAppointmentId());
        } catch (Exception e) {
            log.error("❌ Failed to fetch appointment: {}", e.getMessage(), e);
            throw new AppointmentNotFoundException(
                    "Appointment not found for ID: " + request.getAppointmentId()
            );
        }

        if (appointmentResponse == null || !appointmentResponse.isSuccess()) {
            throw new AppointmentNotFoundException(
                    "Appointment not found or service unavailable for ID: " + request.getAppointmentId()
            );
        }

        AppointmentDto appointment = appointmentResponse.getAppointment();

        // 3️⃣ Null checks & validation
        if (appointment.getPropertyRentAmount() == null) {
            throw new InvalidBookingException("Property rent amount cannot be null");
        }
        if (appointment.getRequesterId() == null) {
            throw new InvalidBookingException("Requester ID cannot be null");
        }
        if (appointment.getProviderId() == null) {
            throw new InvalidBookingException("Provider ID cannot be null");
        }
        if (appointment.getPropertyId() == null) {
            throw new InvalidBookingException("Property ID cannot be null");
        }

        if (!"CONFIRMED".equalsIgnoreCase(String.valueOf(appointment.getStatus())) &&
                !"PENDING".equalsIgnoreCase(String.valueOf(appointment.getStatus()))) {
            throw new InvalidBookingException("Cannot book property. Appointment status: " + appointment.getStatus());
        }

        if (Boolean.TRUE.equals(appointment.getPropertyIsRented())) {
            throw new InvalidBookingException("Property is already rented: " + appointment.getPropertyTitle());
        }

        if (request.getMoveInDate().isBefore(LocalDateTime.now())) {
            throw new InvalidBookingException("Move-in date cannot be in the past");
        }

        if (request.getMoveOutDate().isBefore(request.getMoveInDate())) {
            throw new InvalidBookingException("Move-out date must be after move-in date");
        }

        // 4️⃣ Calculate amounts
        BigDecimal monthlyRent = appointment.getPropertyRentAmount();
        BigDecimal depositAmount = monthlyRent;
        BigDecimal totalAmount = monthlyRent.multiply(BigDecimal.valueOf(request.getBookingDurationMonths()))
                .add(depositAmount);

        // 5️⃣ Build booking entity
        BookingEntity booking = BookingEntity.builder()
                .appointmentId(appointment.getAppointmentId())
                .appointmentTitle(appointment.getAppointmentTitle())
                .appointmentDateTime(appointment.getAppointmentDateTime())
                .propertyId(appointment.getPropertyId())
                .propertyTitle(appointment.getPropertyTitle())
                .propertyAddress(appointment.getPropertyAddress())
                .propertyIsRented(Boolean.TRUE.equals(appointment.getPropertyIsRented()))
                .propertyImage(appointment.getPropertyImage())
                .propertyImage2(appointment.getPropertyImage2())
                .propertyImage3(appointment.getPropertyImage3())
                .propertyImage4(appointment.getPropertyImage4())
                .rentAmount(monthlyRent)
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
                .paymentStatus(PaymentStatus.PENDING)
                .paidAmount(BigDecimal.ZERO)
                .remainingAmount(totalAmount)
                .paymentDeadline(LocalDateTime.now().plusDays(7))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .paymentType(PaymentType.DEPOSIT)
                .notes(request.getNotes())
                .contractSigned(false)
                .emailNotificationSent(false)
                .confirmationToken(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 6️⃣ Save booking safely
        BookingEntity savedBooking;
        try {
            savedBooking = bookingRepository.save(booking);
            log.info("✅ Booking saved successfully: {}", savedBooking.getId());
        } catch (Exception e) {
            log.error("❌ Failed to save booking: {}", e.getMessage(), e);
            throw new BookingPersistenceException("Could not save booking. Check data and try again.");
        }

        // 7️⃣ Publish event safely
        BookingEntity finalSavedBooking = savedBooking;
        publishEventSafely(() -> {
            BookingEvent event = bookingMapper.toEvent(finalSavedBooking);
            if (bookingEventProducer != null) {
                bookingEventProducer.publishBookingCreated(event);
            }
        }, "BOOKING_CREATED");

        // ✅ 8️⃣ START SAGA - This won't cause circular dependency anymore due to @Lazy
        if (sagaOrchestrator != null) {
            try {
                log.info("🎬 Starting BOOKING SAGA for appointment: {}", request.getAppointmentId());
                sagaOrchestrator.startBookingSaga(request.getAppointmentId(), request);
            } catch (Exception e) {
                log.error("❌ Failed to start SAGA: {}", e.getMessage(), e);
                // Don't fail the booking creation if SAGA fails
            }
        } else {
            log.debug("ℹ️ SAGA disabled - skipping saga orchestration");
        }

        return bookingMapper.toDto(savedBooking);
    }

    @Override
    public BookingDto getBookingById(String bookingId) {
        log.info("🔍 Fetching booking: {}", bookingId);
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found: " + bookingId));
        return bookingMapper.toDto(booking);
    }

    @Override
    public List<BookingDto> getAllBookings() {
        log.info("📋 Fetching all bookings");
        List<BookingEntity> bookings = bookingRepository.findAll();
        return bookingMapper.toDtoList(bookings);
    }

    @Override
    public List<BookingDto> getBookingsByAppointmentId(String appointmentId) {
        log.info("🔍 Fetching bookings for appointment: {}", appointmentId);
        List<BookingEntity> bookings = bookingRepository.findByAppointmentId(appointmentId);
        return bookingMapper.toDtoList(bookings);
    }

    @Override
    public BookingDto updatePropertyStatus(String propertyId, boolean propertyIsRented) {
        log.info("🏠 Updating property status for property: {}", propertyId);
        BookingEntity booking = bookingRepository.findById(propertyId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found: " + propertyId));
        booking.setPropertyIsRented(propertyIsRented);
        bookingRepository.save(booking);
        return bookingMapper.toDto(booking);
    }

    @Override
    public List<BookingDto> getBookingsByRequesterId(Long requesterId) {
        log.info("👤 Fetching bookings for requester: {}", requesterId);
        List<BookingEntity> bookings = bookingRepository.findByRequesterId(requesterId);
        return bookingMapper.toDtoList(bookings);
    }

    @Override
    public List<BookingDto> getBookingsByProviderId(Long providerId) {
        log.info("🏢 Fetching bookings for provider: {}", providerId);
        List<BookingEntity> bookings = bookingRepository.findByProviderId(providerId);
        return bookingMapper.toDtoList(bookings);
    }

    @Override
    public List<BookingDto> getBookingsByPropertyId(Long propertyId) {
        log.info("🏘️ Fetching bookings for property: {}", propertyId);
        List<BookingEntity> bookings = bookingRepository.findByPropertyId(propertyId);
        return bookingMapper.toDtoList(bookings);
    }

    @Override
    public List<BookingDto> getBookingsByStatus(BookingStatus status) {
        log.info("📊 Fetching bookings with status: {}", status);
        List<BookingEntity> bookings = bookingRepository.findByStatus(status);
        return bookingMapper.toDtoList(bookings);
    }

    // ========== BOOKING STATUS MANAGEMENT ==========

    @Override
    @Transactional
    public BookingDto updateBookingStatus(String bookingId, BookingStatus status) {
        log.info("🔄 Updating booking {} status to {}", bookingId, status);
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found: " + bookingId));

        BookingStatus oldStatus = booking.getStatus();
        booking.setStatus(status);
        booking.setUpdatedAt(LocalDateTime.now());

        BookingEntity updatedBooking = bookingRepository.save(booking);

        // Publish status update event if status changed
        if (oldStatus != status) {
            BookingEntity finalUpdatedBooking = updatedBooking;
            publishEventSafely(() -> {
                BookingEvent event = bookingMapper.toEvent(finalUpdatedBooking);
                if (status == BookingStatus.CONFIRMED) {
                    bookingEventProducer.publishBookingConfirmed(event);
                } else if (status == BookingStatus.COMPLETED) {
                    bookingEventProducer.publishBookingCompleted(event);
                } else if (status == BookingStatus.EXPIRED) {
                    bookingEventProducer.publishBookingExpired(event);
                }
            }, "BOOKING_STATUS_UPDATE");
        }

        return bookingMapper.toDto(updatedBooking);
    }

    @Override
    public BookingDto cancelBooking(String bookingId, String reason) {
        log.info("❌ Cancelling booking: {}", bookingId);
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
        log.info("✅ Booking cancelled successfully: {}", bookingId);

        // Publish booking cancelled event
        BookingEntity finalCancelledBooking = cancelledBooking;
        publishEventSafely(() -> {
            BookingEvent event = bookingMapper.toEvent(finalCancelledBooking);
            event.setCancelledAt(LocalDateTime.now());
            bookingEventProducer.publishBookingCancelled(event);
        }, "BOOKING_CANCELLED");

        return bookingMapper.toDto(cancelledBooking);
    }

    @Override
    public void deleteBooking(String bookingId) {
        log.info("🗑️ Deleting booking: {}", bookingId);
        if (!bookingRepository.existsById(bookingId)) {
            throw new BookingNotFoundException("Booking not found: " + bookingId);
        }
        bookingRepository.deleteById(bookingId);
        log.info("✅ Booking deleted successfully: {}", bookingId);
    }

    @Override
    public BookingDto confirmBooking(String confirmationToken) {
        log.info("✅ Confirming booking with token: {}", confirmationToken);

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
        log.info("✅ Booking confirmed successfully: {}", confirmedBooking.getId());

        // Publish booking confirmed event
        BookingEntity finalConfirmedBooking = confirmedBooking;
        publishEventSafely(() -> {
            BookingEvent event = bookingMapper.toEvent(finalConfirmedBooking);
            bookingEventProducer.publishBookingConfirmed(event);
        }, "BOOKING_CONFIRMED");

        return bookingMapper.toDto(confirmedBooking);
    }

    // ========== PAYMENT OPERATIONS ==========

    @Override
    public PaymentDto processPayment(ProcessPaymentRequest request) {
        log.info("💳 Processing payment for booking: {}", request.getBookingId());

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
            log.error("❌ Payment processing failed: {}", e.getMessage());
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(e.getMessage());
            booking.setPaymentStatus(PaymentStatus.FAILED);
        }

        PaymentEntity savedPayment = paymentRepository.save(payment);
        BookingEntity updatedBooking = bookingRepository.save(booking);

        // Publish payment completed event if payment was successful
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            PaymentEntity finalSavedPayment = savedPayment;
            BookingEntity finalUpdatedBooking = updatedBooking;
            publishEventSafely(() -> {
                BookingEvent event = bookingMapper.toEvent(finalUpdatedBooking);
                event.setTransactionId(finalSavedPayment.getTransactionId());
                event.setPaymentReference(finalSavedPayment.getPaymentReference());
                bookingEventProducer.publishBookingPaymentCompleted(event);
            }, "BOOKING_PAYMENT_COMPLETED");

            // ✅ UPDATE SAGA - No circular dependency issue due to @Lazy
            if (sagaOrchestrator != null) {
                try {
                    log.info("💰 Updating SAGA - Payment completed for booking: {}", request.getBookingId());
                    sagaOrchestrator.handlePaymentCompleted(
                            request.getBookingId(),
                            finalSavedPayment.getTransactionId()
                    );
                } catch (Exception e) {
                    log.error("❌ Failed to update SAGA: {}", e.getMessage(), e);
                    // Don't fail the payment if SAGA update fails
                }
            }
        }

        log.info("✅ Payment processed successfully: {}", savedPayment.getId());
        return bookingMapper.toDto(savedPayment);
    }

    @Override
    public PaymentDto getPaymentByBookingId(String bookingId) {
        log.info("🔍 Fetching payment for booking: {}", bookingId);
        PaymentEntity payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(
                        "Payment not found for booking: " + bookingId
                ));
        return bookingMapper.toDto(payment);
    }

    @Override
    public List<PaymentDto> getPaymentsByPayerId(Long payerId) {
        log.info("👤 Fetching payments for payer: {}", payerId);
        List<PaymentEntity> payments = paymentRepository.findByPayerId(payerId);
        return bookingMapper.toPaymentDtoList(payments);
    }
}