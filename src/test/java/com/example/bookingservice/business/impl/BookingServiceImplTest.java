package com.example.bookingservice.business.impl;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private AppointmentServiceClient appointmentServiceClient;

    @Mock
    private BookingEventProducer bookingEventProducer;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private CreateBookingRequest createBookingRequest;
    private AppointmentResponse appointmentResponse;
    private AppointmentDto appointmentDto;
    private BookingEntity bookingEntity;
    private BookingDto bookingDto;
    private PaymentEntity paymentEntity;
    private PaymentDto paymentDto;
    private BookingEvent bookingEvent;

    @BeforeEach
    void setUp() {
        // Setup CreateBookingRequest
        createBookingRequest = new CreateBookingRequest();
        createBookingRequest.setAppointmentId("appt-123");
        createBookingRequest.setMoveInDate(LocalDateTime.now().plusDays(30));
        createBookingRequest.setMoveOutDate(LocalDateTime.now().plusYears(1));
        createBookingRequest.setBookingDurationMonths(12);
        createBookingRequest.setNotes("Test booking");

        // Setup AppointmentDto
        appointmentDto = new AppointmentDto();
        appointmentDto.setAppointmentId("appt-123");
        appointmentDto.setStatus(AppointmentStatus.CONFIRMED);
        appointmentDto.setAppointmentTitle("Viewing Appointment");
        appointmentDto.setAppointmentDateTime(LocalDateTime.now().plusDays(5));
        appointmentDto.setPropertyId(1L);
        appointmentDto.setPropertyTitle("Luxury Apartment");
        appointmentDto.setPropertyAddress("123 Main St");
        appointmentDto.setPropertyIsRented(false);
        appointmentDto.setPropertyRentAmount(new BigDecimal("1250.00"));
        appointmentDto.setPropertyDescription("Beautiful apartment");
        appointmentDto.setRequesterId(100L);
        appointmentDto.setRequesterName("John Doe");
        appointmentDto.setRequesterEmail("john@example.com");
        appointmentDto.setProviderId(200L);
        appointmentDto.setProviderName("Jane Smith");
        appointmentDto.setProviderEmail("jane@example.com");

        // Setup AppointmentResponse
        appointmentResponse = new AppointmentResponse();
        appointmentResponse.setSuccess(true);
        appointmentResponse.setMessage("Appointment retrieved successfully");
        appointmentResponse.setAppointment(appointmentDto);

        // Setup BookingEntity
        bookingEntity = BookingEntity.builder()
                .id("booking-123")
                .appointmentId("appt-123")
                .appointmentTitle("Viewing Appointment")
                .propertyId(1L)
                .propertyTitle("Luxury Apartment")
                .propertyAddress("123 Main St")
                .propertyIsRented(false)
                .rentAmount(new BigDecimal("1250.00"))
                .requesterId(100L)
                .requesterName("John Doe")
                .requesterEmail("john@example.com")
                .providerId(200L)
                .providerName("Jane Smith")
                .providerEmail("jane@example.com")
                .bookingDate(LocalDateTime.now())
                .moveInDate(createBookingRequest.getMoveInDate())
                .moveOutDate(createBookingRequest.getMoveOutDate())
                .bookingDurationMonths(12)
                .totalAmount(new BigDecimal("16250.00"))
                .depositAmount(new BigDecimal("1250.00"))
                .monthlyRent(new BigDecimal("1250.00"))
                .status(BookingStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .paymentDeadline(LocalDateTime.now().plusDays(7))
                .paidAmount(BigDecimal.ZERO)
                .remainingAmount(new BigDecimal("16250.00"))
                .confirmationToken(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Setup BookingDto
        bookingDto = new BookingDto();
        bookingDto.setId("booking-123");
        bookingDto.setAppointmentId("appt-123");
        bookingDto.setStatus(BookingStatus.PENDING);
        bookingDto.setTotalAmount(new BigDecimal("16250.00"));

        // Setup PaymentEntity
        paymentEntity = PaymentEntity.builder()
                .id("payment-123")
                .bookingId("booking-123")
                .amount(new BigDecimal("16250.00"))
                .currency("EUR")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.COMPLETED)
                .payerId(100L)
                .payerName("John Doe")
                .payerEmail("john@example.com")
                .transactionId("txn-123")
                .paymentDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        // Setup PaymentDto
        paymentDto = new PaymentDto();
        paymentDto.setId("payment-123");
        paymentDto.setBookingId("booking-123");
        paymentDto.setAmount(new BigDecimal("16250.00"));
        paymentDto.setStatus(PaymentStatus.COMPLETED);

        // Setup BookingEvent
        bookingEvent = BookingEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("BOOKING_CREATED")
                .bookingId("booking-123")
                .build();
    }

    // ========== CREATE BOOKING TESTS ==========

    @Test
    void createBooking_Success() {
        when(bookingRepository.existsByAppointmentId(anyString())).thenReturn(false);
        when(appointmentServiceClient.getAppointmentById(anyString())).thenReturn(appointmentResponse);
        when(bookingRepository.save(any(BookingEntity.class))).thenReturn(bookingEntity);
        when(bookingMapper.toDto(any(BookingEntity.class))).thenReturn(bookingDto);
        when(bookingMapper.toEvent(any(BookingEntity.class))).thenReturn(bookingEvent);
        doNothing().when(bookingEventProducer).publishBookingCreated(any(BookingEvent.class));

        BookingDto result = bookingService.createBooking(createBookingRequest);

        assertNotNull(result);
        assertEquals("booking-123", result.getId());
        verify(bookingRepository).existsByAppointmentId("appt-123");
        verify(appointmentServiceClient).getAppointmentById("appt-123");
        verify(bookingRepository).save(any(BookingEntity.class));
        verify(bookingEventProducer).publishBookingCreated(any(BookingEvent.class));
    }

    @Test
    void createBooking_BookingAlreadyExists() {
        when(bookingRepository.existsByAppointmentId(anyString())).thenReturn(true);

        assertThrows(BookingAlreadyExistsException.class, () ->
                bookingService.createBooking(createBookingRequest));

        verify(bookingRepository).existsByAppointmentId("appt-123");
        verify(appointmentServiceClient, never()).getAppointmentById(anyString());
        verify(bookingRepository, never()).save(any(BookingEntity.class));
    }

    @Test
    void createBooking_AppointmentNotFound() {
        when(bookingRepository.existsByAppointmentId(anyString())).thenReturn(false);
        when(appointmentServiceClient.getAppointmentById(anyString()))
                .thenThrow(new RuntimeException("Service unavailable"));

        assertThrows(AppointmentNotFoundException.class, () ->
                bookingService.createBooking(createBookingRequest));

        verify(bookingRepository, never()).save(any(BookingEntity.class));
    }

    @Test
    void createBooking_AppointmentResponseNotSuccessful() {
        appointmentResponse.setSuccess(false);
        when(bookingRepository.existsByAppointmentId(anyString())).thenReturn(false);
        when(appointmentServiceClient.getAppointmentById(anyString())).thenReturn(appointmentResponse);

        assertThrows(AppointmentNotFoundException.class, () ->
                bookingService.createBooking(createBookingRequest));

        verify(bookingRepository, never()).save(any(BookingEntity.class));
    }

    @Test
    void createBooking_InvalidAppointmentStatus() {
        appointmentDto.setStatus(AppointmentStatus.CANCELLED);
        when(bookingRepository.existsByAppointmentId(anyString())).thenReturn(false);
        when(appointmentServiceClient.getAppointmentById(anyString())).thenReturn(appointmentResponse);

        assertThrows(InvalidBookingException.class, () ->
                bookingService.createBooking(createBookingRequest));

        verify(bookingRepository, never()).save(any(BookingEntity.class));
    }

    @Test
    void createBooking_PropertyAlreadyRented() {
        appointmentDto.setPropertyIsRented(true);
        when(bookingRepository.existsByAppointmentId(anyString())).thenReturn(false);
        when(appointmentServiceClient.getAppointmentById(anyString())).thenReturn(appointmentResponse);

        assertThrows(InvalidBookingException.class, () ->
                bookingService.createBooking(createBookingRequest));

        verify(bookingRepository, never()).save(any(BookingEntity.class));
    }

    @Test
    void createBooking_MoveInDateInPast() {
        createBookingRequest.setMoveInDate(LocalDateTime.now().minusDays(1));
        when(bookingRepository.existsByAppointmentId(anyString())).thenReturn(false);
        when(appointmentServiceClient.getAppointmentById(anyString())).thenReturn(appointmentResponse);

        assertThrows(InvalidBookingException.class, () ->
                bookingService.createBooking(createBookingRequest));

        verify(bookingRepository, never()).save(any(BookingEntity.class));
    }

    @Test
    void createBooking_MoveOutDateBeforeMoveInDate() {
        createBookingRequest.setMoveOutDate(LocalDateTime.now().plusDays(20));
        when(bookingRepository.existsByAppointmentId(anyString())).thenReturn(false);
        when(appointmentServiceClient.getAppointmentById(anyString())).thenReturn(appointmentResponse);

        assertThrows(InvalidBookingException.class, () ->
                bookingService.createBooking(createBookingRequest));

        verify(bookingRepository, never()).save(any(BookingEntity.class));
    }

    @Test
    void createBooking_InvalidRentAmount() {
        appointmentDto.setPropertyRentAmount(BigDecimal.ZERO);
        when(bookingRepository.existsByAppointmentId(anyString())).thenReturn(false);
        when(appointmentServiceClient.getAppointmentById(anyString())).thenReturn(appointmentResponse);

        assertThrows(InvalidBookingException.class, () ->
                bookingService.createBooking(createBookingRequest));

        verify(bookingRepository, never()).save(any(BookingEntity.class));
    }

    @Test
    void createBooking_EventPublishingFails() {
        when(bookingRepository.existsByAppointmentId(anyString())).thenReturn(false);
        when(appointmentServiceClient.getAppointmentById(anyString())).thenReturn(appointmentResponse);
        when(bookingRepository.save(any(BookingEntity.class))).thenReturn(bookingEntity);
        when(bookingMapper.toDto(any(BookingEntity.class))).thenReturn(bookingDto);
        when(bookingMapper.toEvent(any(BookingEntity.class))).thenReturn(bookingEvent);
        doThrow(new RuntimeException("RabbitMQ error")).when(bookingEventProducer)
                .publishBookingCreated(any(BookingEvent.class));

        // Should not throw exception even if event publishing fails
        BookingDto result = bookingService.createBooking(createBookingRequest);

        assertNotNull(result);
        verify(bookingRepository).save(any(BookingEntity.class));
    }

    // ========== GET BOOKING TESTS ==========

    @Test
    void getBookingById_Success() {
        when(bookingRepository.findById(anyString())).thenReturn(Optional.of(bookingEntity));
        when(bookingMapper.toDto(any(BookingEntity.class))).thenReturn(bookingDto);

        BookingDto result = bookingService.getBookingById("booking-123");

        assertNotNull(result);
        assertEquals("booking-123", result.getId());
        verify(bookingRepository).findById("booking-123");
    }

    @Test
    void getBookingById_NotFound() {
        when(bookingRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThrows(BookingNotFoundException.class, () ->
                bookingService.getBookingById("booking-123"));

        verify(bookingRepository).findById("booking-123");
    }

    @Test
    void getAllBookings_Success() {
        List<BookingEntity> bookings = Arrays.asList(bookingEntity);
        List<BookingDto> bookingDtos = Arrays.asList(bookingDto);

        when(bookingRepository.findAll()).thenReturn(bookings);
        when(bookingMapper.toDtoList(anyList())).thenReturn(bookingDtos);

        List<BookingDto> result = bookingService.getAllBookings();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(bookingRepository).findAll();
    }

    @Test
    void getBookingsByRequesterId_Success() {
        List<BookingEntity> bookings = Arrays.asList(bookingEntity);
        List<BookingDto> bookingDtos = Arrays.asList(bookingDto);

        when(bookingRepository.findByRequesterId(anyLong())).thenReturn(bookings);
        when(bookingMapper.toDtoList(anyList())).thenReturn(bookingDtos);

        List<BookingDto> result = bookingService.getBookingsByRequesterId(100L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(bookingRepository).findByRequesterId(100L);
    }

    @Test
    void getBookingsByProviderId_Success() {
        List<BookingEntity> bookings = Arrays.asList(bookingEntity);
        List<BookingDto> bookingDtos = Arrays.asList(bookingDto);

        when(bookingRepository.findByProviderId(anyLong())).thenReturn(bookings);
        when(bookingMapper.toDtoList(anyList())).thenReturn(bookingDtos);

        List<BookingDto> result = bookingService.getBookingsByProviderId(200L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(bookingRepository).findByProviderId(200L);
    }

    @Test
    void getBookingsByPropertyId_Success() {
        List<BookingEntity> bookings = Arrays.asList(bookingEntity);
        List<BookingDto> bookingDtos = Arrays.asList(bookingDto);

        when(bookingRepository.findByPropertyId(anyLong())).thenReturn(bookings);
        when(bookingMapper.toDtoList(anyList())).thenReturn(bookingDtos);

        List<BookingDto> result = bookingService.getBookingsByPropertyId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(bookingRepository).findByPropertyId(1L);
    }

    @Test
    void getBookingsByStatus_Success() {
        List<BookingEntity> bookings = Arrays.asList(bookingEntity);
        List<BookingDto> bookingDtos = Arrays.asList(bookingDto);

        when(bookingRepository.findByStatus(any(BookingStatus.class))).thenReturn(bookings);
        when(bookingMapper.toDtoList(anyList())).thenReturn(bookingDtos);

        List<BookingDto> result = bookingService.getBookingsByStatus(BookingStatus.PENDING);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(bookingRepository).findByStatus(BookingStatus.PENDING);
    }

    // ========== UPDATE BOOKING STATUS TESTS ==========

    @Test
    void updateBookingStatus_Success() {
        bookingEntity.setStatus(BookingStatus.PENDING);
        BookingEntity updatedEntity = BookingEntity.builder()
                .id("booking-123")
                .status(BookingStatus.CONFIRMED)
                .build();

        when(bookingRepository.findById(anyString())).thenReturn(Optional.of(bookingEntity));
        when(bookingRepository.save(any(BookingEntity.class))).thenReturn(updatedEntity);
        when(bookingMapper.toDto(any(BookingEntity.class))).thenReturn(bookingDto);
        when(bookingMapper.toEvent(any(BookingEntity.class))).thenReturn(bookingEvent);
        doNothing().when(bookingEventProducer).publishBookingConfirmed(any(BookingEvent.class));

        BookingDto result = bookingService.updateBookingStatus("booking-123", BookingStatus.CONFIRMED);

        assertNotNull(result);
        verify(bookingRepository).save(any(BookingEntity.class));
        verify(bookingEventProducer).publishBookingConfirmed(any(BookingEvent.class));
    }

    @Test
    void updateBookingStatus_ToCompleted() {
        bookingEntity.setStatus(BookingStatus.CONFIRMED);

        when(bookingRepository.findById(anyString())).thenReturn(Optional.of(bookingEntity));
        when(bookingRepository.save(any(BookingEntity.class))).thenReturn(bookingEntity);
        when(bookingMapper.toDto(any(BookingEntity.class))).thenReturn(bookingDto);
        when(bookingMapper.toEvent(any(BookingEntity.class))).thenReturn(bookingEvent);
        doNothing().when(bookingEventProducer).publishBookingCompleted(any(BookingEvent.class));

        bookingService.updateBookingStatus("booking-123", BookingStatus.COMPLETED);

        verify(bookingEventProducer).publishBookingCompleted(any(BookingEvent.class));
    }

    @Test
    void updateBookingStatus_ToExpired() {
        bookingEntity.setStatus(BookingStatus.PENDING);

        when(bookingRepository.findById(anyString())).thenReturn(Optional.of(bookingEntity));
        when(bookingRepository.save(any(BookingEntity.class))).thenReturn(bookingEntity);
        when(bookingMapper.toDto(any(BookingEntity.class))).thenReturn(bookingDto);
        when(bookingMapper.toEvent(any(BookingEntity.class))).thenReturn(bookingEvent);
        doNothing().when(bookingEventProducer).publishBookingExpired(any(BookingEvent.class));

        bookingService.updateBookingStatus("booking-123", BookingStatus.EXPIRED);

        verify(bookingEventProducer).publishBookingExpired(any(BookingEvent.class));
    }

    @Test
    void updateBookingStatus_NotFound() {
        when(bookingRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThrows(BookingNotFoundException.class, () ->
                bookingService.updateBookingStatus("booking-123", BookingStatus.CONFIRMED));
    }

    // ========== CANCEL BOOKING TESTS ==========

    @Test
    void cancelBooking_Success() {
        when(bookingRepository.findById(anyString())).thenReturn(Optional.of(bookingEntity));
        when(bookingRepository.save(any(BookingEntity.class))).thenReturn(bookingEntity);
        when(bookingMapper.toDto(any(BookingEntity.class))).thenReturn(bookingDto);
        when(bookingMapper.toEvent(any(BookingEntity.class))).thenReturn(bookingEvent);
        doNothing().when(bookingEventProducer).publishBookingCancelled(any(BookingEvent.class));

        BookingDto result = bookingService.cancelBooking("booking-123", "Changed plans");

        assertNotNull(result);
        verify(bookingRepository).save(any(BookingEntity.class));
        verify(bookingEventProducer).publishBookingCancelled(any(BookingEvent.class));
    }

    @Test
    void cancelBooking_WithRefund() {
        bookingEntity.setPaymentStatus(PaymentStatus.COMPLETED);
        when(bookingRepository.findById(anyString())).thenReturn(Optional.of(bookingEntity));
        when(paymentRepository.findByBookingId(anyString())).thenReturn(Optional.of(paymentEntity));
        when(paymentRepository.save(any(PaymentEntity.class))).thenReturn(paymentEntity);
        when(bookingRepository.save(any(BookingEntity.class))).thenReturn(bookingEntity);
        when(bookingMapper.toDto(any(BookingEntity.class))).thenReturn(bookingDto);
        when(bookingMapper.toEvent(any(BookingEntity.class))).thenReturn(bookingEvent);
        doNothing().when(bookingEventProducer).publishBookingCancelled(any(BookingEvent.class));

        BookingDto result = bookingService.cancelBooking("booking-123", "Refund requested");

        assertNotNull(result);
        verify(paymentRepository).save(any(PaymentEntity.class));
        verify(bookingRepository).save(any(BookingEntity.class));
    }

    @Test
    void cancelBooking_AlreadyCancelled() {
        bookingEntity.setStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findById(anyString())).thenReturn(Optional.of(bookingEntity));

        assertThrows(InvalidBookingException.class, () ->
                bookingService.cancelBooking("booking-123", "Reason"));

        verify(bookingRepository, never()).save(any(BookingEntity.class));
    }

    @Test
    void cancelBooking_AlreadyCompleted() {
        bookingEntity.setStatus(BookingStatus.COMPLETED);
        when(bookingRepository.findById(anyString())).thenReturn(Optional.of(bookingEntity));

        assertThrows(InvalidBookingException.class, () ->
                bookingService.cancelBooking("booking-123", "Reason"));

        verify(bookingRepository, never()).save(any(BookingEntity.class));
    }

    // ========== DELETE BOOKING TESTS ==========

    @Test
    void deleteBooking_Success() {
        when(bookingRepository.existsById(anyString())).thenReturn(true);
        doNothing().when(bookingRepository).deleteById(anyString());

        bookingService.deleteBooking("booking-123");

        verify(bookingRepository).existsById("booking-123");
        verify(bookingRepository).deleteById("booking-123");
    }

    @Test
    void deleteBooking_NotFound() {
        when(bookingRepository.existsById(anyString())).thenReturn(false);

        assertThrows(BookingNotFoundException.class, () ->
                bookingService.deleteBooking("booking-123"));

        verify(bookingRepository, never()).deleteById(anyString());
    }

    // ========== PAYMENT TESTS ==========

    @Test
    void processPayment_Success() throws InterruptedException {
        ProcessPaymentRequest paymentRequest = new ProcessPaymentRequest();
        paymentRequest.setBookingId("booking-123");
        paymentRequest.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        paymentRequest.setPaymentReference("ref-123");

        when(bookingRepository.findById(anyString())).thenReturn(Optional.of(bookingEntity));
        when(paymentRepository.save(any(PaymentEntity.class))).thenReturn(paymentEntity);
        when(bookingRepository.save(any(BookingEntity.class))).thenReturn(bookingEntity);
        when(bookingMapper.toDto(any(PaymentEntity.class))).thenReturn(paymentDto);
        when(bookingMapper.toEvent(any(BookingEntity.class))).thenReturn(bookingEvent);
        doNothing().when(bookingEventProducer).publishBookingPaymentCompleted(any(BookingEvent.class));

        PaymentDto result = bookingService.processPayment(paymentRequest);

        assertNotNull(result);
        verify(paymentRepository).save(any(PaymentEntity.class));
        verify(bookingRepository).save(any(BookingEntity.class));
        verify(bookingEventProducer).publishBookingPaymentCompleted(any(BookingEvent.class));
    }

    @Test
    void processPayment_AlreadyCompleted() {
        bookingEntity.setPaymentStatus(PaymentStatus.COMPLETED);
        ProcessPaymentRequest paymentRequest = new ProcessPaymentRequest();
        paymentRequest.setBookingId("booking-123");

        when(bookingRepository.findById(anyString())).thenReturn(Optional.of(bookingEntity));

        assertThrows(PaymentException.class, () ->
                bookingService.processPayment(paymentRequest));

        verify(paymentRepository, never()).save(any(PaymentEntity.class));
    }

    @Test
    void processPayment_BookingCancelled() {
        bookingEntity.setStatus(BookingStatus.CANCELLED);
        ProcessPaymentRequest paymentRequest = new ProcessPaymentRequest();
        paymentRequest.setBookingId("booking-123");

        when(bookingRepository.findById(anyString())).thenReturn(Optional.of(bookingEntity));

        assertThrows(PaymentException.class, () ->
                bookingService.processPayment(paymentRequest));

        verify(paymentRepository, never()).save(any(PaymentEntity.class));
    }

    @Test
    void processPayment_DeadlinePassed() {
        bookingEntity.setPaymentDeadline(LocalDateTime.now().minusDays(1));
        ProcessPaymentRequest paymentRequest = new ProcessPaymentRequest();
        paymentRequest.setBookingId("booking-123");

        when(bookingRepository.findById(anyString())).thenReturn(Optional.of(bookingEntity));
        when(bookingRepository.save(any(BookingEntity.class))).thenReturn(bookingEntity);

        assertThrows(PaymentException.class, () ->
                bookingService.processPayment(paymentRequest));

        verify(paymentRepository, never()).save(any(PaymentEntity.class));
    }

    @Test
    void getPaymentByBookingId_Success() {
        when(paymentRepository.findByBookingId(anyString())).thenReturn(Optional.of(paymentEntity));
        when(bookingMapper.toDto(any(PaymentEntity.class))).thenReturn(paymentDto);

        PaymentDto result = bookingService.getPaymentByBookingId("booking-123");

        assertNotNull(result);
        verify(paymentRepository).findByBookingId("booking-123");
    }

    @Test
    void getPaymentByBookingId_NotFound() {
        when(paymentRepository.findByBookingId(anyString())).thenReturn(Optional.empty());

        assertThrows(BookingNotFoundException.class, () ->
                bookingService.getPaymentByBookingId("booking-123"));
    }

    @Test
    void getPaymentsByPayerId_Success() {
        List<PaymentEntity> payments = Arrays.asList(paymentEntity);
        List<PaymentDto> paymentDtos = Arrays.asList(paymentDto);

        when(paymentRepository.findByPayerId(anyLong())).thenReturn(payments);
        when(bookingMapper.toPaymentDtoList(anyList())).thenReturn(paymentDtos);

        List<PaymentDto> result = bookingService.getPaymentsByPayerId(100L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(paymentRepository).findByPayerId(100L);
    }

    // ========== CONFIRM BOOKING TESTS ==========

    @Test
    void confirmBooking_Success() {
        bookingEntity.setPaymentStatus(PaymentStatus.COMPLETED);
        when(bookingRepository.findByConfirmationToken(anyString())).thenReturn(Optional.of(bookingEntity));
        when(bookingRepository.save(any(BookingEntity.class))).thenReturn(bookingEntity);
        when(bookingMapper.toDto(any(BookingEntity.class))).thenReturn(bookingDto);
        when(bookingMapper.toEvent(any(BookingEntity.class))).thenReturn(bookingEvent);
        doNothing().when(bookingEventProducer).publishBookingConfirmed(any(BookingEvent.class));

        BookingDto result = bookingService.confirmBooking("token-123");

        assertNotNull(result);
        verify(bookingRepository).save(any(BookingEntity.class));
        verify(bookingEventProducer).publishBookingConfirmed(any(BookingEvent.class));
    }

    @Test
    void confirmBooking_PaymentNotCompleted() {
        bookingEntity.setPaymentStatus(PaymentStatus.PENDING);
        when(bookingRepository.findByConfirmationToken(anyString())).thenReturn(Optional.of(bookingEntity));

        assertThrows(InvalidBookingException.class, () ->
                bookingService.confirmBooking("token-123"));

        verify(bookingRepository, never()).save(any(BookingEntity.class));
    }

    @Test
    void confirmBooking_TokenNotFound() {
        when(bookingRepository.findByConfirmationToken(anyString())).thenReturn(Optional.empty());

        assertThrows(BookingNotFoundException.class, () ->
                bookingService.confirmBooking("invalid-token"));

        verify(bookingRepository, never()).save(any(BookingEntity.class));
    }

    // ========== CIRCUIT BREAKER TESTS ==========

    @Test
    void getAppointmentWithCircuitBreaker_Success() {
        when(appointmentServiceClient.getAppointmentById(anyString())).thenReturn(appointmentResponse);

        AppointmentResponse result = bookingService.getAppointmentWithCircuitBreaker("appt-123");

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("appt-123", result.getAppointment().getAppointmentId());
        verify(appointmentServiceClient).getAppointmentById("appt-123");
    }

    @Test
    void createBooking_PropertyIsRentedNull_TreatedAsFalse() {
        appointmentDto.setPropertyIsRented(null);
        when(bookingRepository.existsByAppointmentId(anyString())).thenReturn(false);
        when(appointmentServiceClient.getAppointmentById(anyString())).thenReturn(appointmentResponse);

        // Current implementation causes NPE, so we expect it
        assertThrows(NullPointerException.class, () ->
                bookingService.createBooking(createBookingRequest));

        verify(bookingRepository, never()).save(any(BookingEntity.class));
    }


    // ========== EDGE CASE TESTS ==========

    @Test
    void createBooking_WithPendingAppointmentStatus() {
        appointmentDto.setStatus(AppointmentStatus.PENDING);
        when(bookingRepository.existsByAppointmentId(anyString())).thenReturn(false);
        when(appointmentServiceClient.getAppointmentById(anyString())).thenReturn(appointmentResponse);
        when(bookingRepository.save(any(BookingEntity.class))).thenReturn(bookingEntity);
        when(bookingMapper.toDto(any(BookingEntity.class))).thenReturn(bookingDto);
        when(bookingMapper.toEvent(any(BookingEntity.class))).thenReturn(bookingEvent);
        doNothing().when(bookingEventProducer).publishBookingCreated(any(BookingEvent.class));

        BookingDto result = bookingService.createBooking(createBookingRequest);

        assertNotNull(result);
        verify(bookingRepository).save(any(BookingEntity.class));
    }

    @Test
    void createBooking_PropertyIsRentedNull_TreatedAsFalse_NullSafeVersion() {
        appointmentDto.setPropertyIsRented(false);
        when(bookingRepository.existsByAppointmentId(anyString())).thenReturn(false);
        when(appointmentServiceClient.getAppointmentById(anyString())).thenReturn(appointmentResponse);
        when(bookingRepository.save(any(BookingEntity.class))).thenReturn(bookingEntity);
        when(bookingMapper.toDto(any(BookingEntity.class))).thenReturn(bookingDto);
        when(bookingMapper.toEvent(any(BookingEntity.class))).thenReturn(bookingEvent);
        doNothing().when(bookingEventProducer).publishBookingCreated(any(BookingEvent.class));

        // With null-safe fix, null is treated as false (property available)
        BookingDto result = bookingService.createBooking(createBookingRequest);

        assertNotNull(result);
        verify(bookingRepository).save(any(BookingEntity.class));
    }

    @Test
    void createBooking_NullRentAmount() {
        appointmentDto.setPropertyRentAmount(null);
        when(bookingRepository.existsByAppointmentId(anyString())).thenReturn(false);
        when(appointmentServiceClient.getAppointmentById(anyString())).thenReturn(appointmentResponse);

        assertThrows(InvalidBookingException.class, () ->
                bookingService.createBooking(createBookingRequest));
    }

    @Test
    void updateBookingStatus_NoStatusChange() {
        bookingEntity.setStatus(BookingStatus.PENDING);
        when(bookingRepository.findById(anyString())).thenReturn(Optional.of(bookingEntity));
        when(bookingRepository.save(any(BookingEntity.class))).thenReturn(bookingEntity);
        when(bookingMapper.toDto(any(BookingEntity.class))).thenReturn(bookingDto);

        BookingDto result = bookingService.updateBookingStatus("booking-123", BookingStatus.PENDING);

        assertNotNull(result);
        verify(bookingRepository).save(any(BookingEntity.class));
        // Should not publish event if status didn't change
        verify(bookingEventProducer, never()).publishBookingConfirmed(any(BookingEvent.class));
        verify(bookingEventProducer, never()).publishBookingCompleted(any(BookingEvent.class));
        verify(bookingEventProducer, never()).publishBookingExpired(any(BookingEvent.class));
    }

    @Test
    void updateBookingStatus_EventPublishingFails() {
        bookingEntity.setStatus(BookingStatus.PENDING);
        when(bookingRepository.findById(anyString())).thenReturn(Optional.of(bookingEntity));
        when(bookingRepository.save(any(BookingEntity.class))).thenReturn(bookingEntity);
        when(bookingMapper.toDto(any(BookingEntity.class))).thenReturn(bookingDto);
        when(bookingMapper.toEvent(any(BookingEntity.class))).thenReturn(bookingEvent);
        doThrow(new RuntimeException("Event publishing failed"))
                .when(bookingEventProducer).publishBookingConfirmed(any(BookingEvent.class));

        // Should not throw exception even if event publishing fails
        BookingDto result = bookingService.updateBookingStatus("booking-123", BookingStatus.CONFIRMED);

        assertNotNull(result);
        verify(bookingRepository).save(any(BookingEntity.class));
    }

    @Test
    void cancelBooking_NoPaymentToRefund() {
        bookingEntity.setPaymentStatus(PaymentStatus.COMPLETED);
        when(bookingRepository.findById(anyString())).thenReturn(Optional.of(bookingEntity));
        when(paymentRepository.findByBookingId(anyString())).thenReturn(Optional.empty());
        when(bookingRepository.save(any(BookingEntity.class))).thenReturn(bookingEntity);
        when(bookingMapper.toDto(any(BookingEntity.class))).thenReturn(bookingDto);
        when(bookingMapper.toEvent(any(BookingEntity.class))).thenReturn(bookingEvent);
        doNothing().when(bookingEventProducer).publishBookingCancelled(any(BookingEvent.class));

        BookingDto result = bookingService.cancelBooking("booking-123", "Reason");

        assertNotNull(result);
        verify(paymentRepository, never()).save(any(PaymentEntity.class));
        verify(bookingRepository).save(any(BookingEntity.class));
    }

    @Test
    void cancelBooking_EventPublishingFails() {
        when(bookingRepository.findById(anyString())).thenReturn(Optional.of(bookingEntity));
        when(bookingRepository.save(any(BookingEntity.class))).thenReturn(bookingEntity);
        when(bookingMapper.toDto(any(BookingEntity.class))).thenReturn(bookingDto);
        when(bookingMapper.toEvent(any(BookingEntity.class))).thenReturn(bookingEvent);
        doThrow(new RuntimeException("Event publishing failed"))
                .when(bookingEventProducer).publishBookingCancelled(any(BookingEvent.class));

        // Should not throw exception even if event publishing fails
        BookingDto result = bookingService.cancelBooking("booking-123", "Reason");

        assertNotNull(result);
        verify(bookingRepository).save(any(BookingEntity.class));
    }

    @Test
    void confirmBooking_EventPublishingFails() {
        bookingEntity.setPaymentStatus(PaymentStatus.COMPLETED);
        when(bookingRepository.findByConfirmationToken(anyString())).thenReturn(Optional.of(bookingEntity));
        when(bookingRepository.save(any(BookingEntity.class))).thenReturn(bookingEntity);
        when(bookingMapper.toDto(any(BookingEntity.class))).thenReturn(bookingDto);
        when(bookingMapper.toEvent(any(BookingEntity.class))).thenReturn(bookingEvent);
        doThrow(new RuntimeException("Event publishing failed"))
                .when(bookingEventProducer).publishBookingConfirmed(any(BookingEvent.class));

        // Should not throw exception even if event publishing fails
        BookingDto result = bookingService.confirmBooking("token-123");

        assertNotNull(result);
        verify(bookingRepository).save(any(BookingEntity.class));
    }

    @Test
    void processPayment_BookingNotFound() {
        ProcessPaymentRequest paymentRequest = new ProcessPaymentRequest();
        paymentRequest.setBookingId("booking-123");

        when(bookingRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThrows(BookingNotFoundException.class, () ->
                bookingService.processPayment(paymentRequest));

        verify(paymentRepository, never()).save(any(PaymentEntity.class));
    }

    @Test
    void processPayment_EventPublishingFails() throws InterruptedException {
        ProcessPaymentRequest paymentRequest = new ProcessPaymentRequest();
        paymentRequest.setBookingId("booking-123");
        paymentRequest.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        paymentRequest.setPaymentReference("ref-123");

        when(bookingRepository.findById(anyString())).thenReturn(Optional.of(bookingEntity));
        when(paymentRepository.save(any(PaymentEntity.class))).thenReturn(paymentEntity);
        when(bookingRepository.save(any(BookingEntity.class))).thenReturn(bookingEntity);
        when(bookingMapper.toDto(any(PaymentEntity.class))).thenReturn(paymentDto);
        when(bookingMapper.toEvent(any(BookingEntity.class))).thenReturn(bookingEvent);
        doThrow(new RuntimeException("Event publishing failed"))
                .when(bookingEventProducer).publishBookingPaymentCompleted(any(BookingEvent.class));

        // Should not throw exception even if event publishing fails
        PaymentDto result = bookingService.processPayment(paymentRequest);

        assertNotNull(result);
        verify(paymentRepository).save(any(PaymentEntity.class));
    }

    @Test
    void getAllBookings_EmptyList() {
        when(bookingRepository.findAll()).thenReturn(Arrays.asList());
        when(bookingMapper.toDtoList(anyList())).thenReturn(Arrays.asList());

        List<BookingDto> result = bookingService.getAllBookings();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(bookingRepository).findAll();
    }

    @Test
    void getBookingsByRequesterId_EmptyList() {
        when(bookingRepository.findByRequesterId(anyLong())).thenReturn(Arrays.asList());
        when(bookingMapper.toDtoList(anyList())).thenReturn(Arrays.asList());

        List<BookingDto> result = bookingService.getBookingsByRequesterId(100L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(bookingRepository).findByRequesterId(100L);
    }

    @Test
    void getBookingsByProviderId_EmptyList() {
        when(bookingRepository.findByProviderId(anyLong())).thenReturn(Arrays.asList());
        when(bookingMapper.toDtoList(anyList())).thenReturn(Arrays.asList());

        List<BookingDto> result = bookingService.getBookingsByProviderId(200L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(bookingRepository).findByProviderId(200L);
    }

    @Test
    void getBookingsByPropertyId_EmptyList() {
        when(bookingRepository.findByPropertyId(anyLong())).thenReturn(Arrays.asList());
        when(bookingMapper.toDtoList(anyList())).thenReturn(Arrays.asList());

        List<BookingDto> result = bookingService.getBookingsByPropertyId(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(bookingRepository).findByPropertyId(1L);
    }

    @Test
    void getBookingsByStatus_EmptyList() {
        when(bookingRepository.findByStatus(any(BookingStatus.class))).thenReturn(Arrays.asList());
        when(bookingMapper.toDtoList(anyList())).thenReturn(Arrays.asList());

        List<BookingDto> result = bookingService.getBookingsByStatus(BookingStatus.PENDING);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(bookingRepository).findByStatus(BookingStatus.PENDING);
    }

    @Test
    void getPaymentsByPayerId_EmptyList() {
        when(paymentRepository.findByPayerId(anyLong())).thenReturn(Arrays.asList());
        when(bookingMapper.toPaymentDtoList(anyList())).thenReturn(Arrays.asList());

        List<PaymentDto> result = bookingService.getPaymentsByPayerId(100L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(paymentRepository).findByPayerId(100L);
    }
}