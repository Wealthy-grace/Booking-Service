package com.example.bookingservice.controller;

import com.example.bookingservice.business.interfaces.BookingService;
import com.example.bookingservice.domain.dto.BookingDto;
import com.example.bookingservice.domain.dto.PaymentDto;
import com.example.bookingservice.domain.request.CreateBookingRequest;
import com.example.bookingservice.domain.request.ProcessPaymentRequest;
import com.example.bookingservice.persistence.model.BookingStatus;
import com.example.bookingservice.persistence.model.PaymentMethod;
import com.example.bookingservice.persistence.model.PaymentStatus;
import com.example.bookingservice.persistence.model.PaymentType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


/**
 * Alternative test configuration that loads ONLY the controller
 * without the main application class
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = BookingController.class)
@ContextConfiguration(classes = {BookingController.class})
@AutoConfigureMockMvc(addFilters = false)
public class BookingControllerTestAlternative {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    private BookingDto testBookingDto;
    private CreateBookingRequest createBookingRequest;
    private PaymentDto testPaymentDto;
    private ProcessPaymentRequest processPaymentRequest;

    @BeforeEach
    void setUp() {
        testBookingDto = BookingDto.builder()
                .id("booking-123")
                .appointmentId("appt-123")
                .requesterId(1L)
                .providerId(2L)
                .propertyId(100L)
                .status(BookingStatus.PENDING)
                .totalAmount(new BigDecimal("2500.00"))
                .monthlyRent(new BigDecimal("1250.00"))
                .depositAmount(new BigDecimal("1250.00"))
                .createdAt(LocalDateTime.now())
                .build();

        // FIX: Add moveOutDate to the request
        createBookingRequest = CreateBookingRequest.builder()
                .appointmentId("appt-123")
                .bookingDurationMonths(12)
                .moveInDate(LocalDateTime.now().plusDays(30))
                .moveOutDate(LocalDateTime.now().plusMonths(12).plusDays(30))  // ✅ ADDED THIS
                .build();

        testPaymentDto = PaymentDto.builder()
                .id("payment-123")
                .bookingId("booking-123")
                .payerId(1L)
                .amount(new BigDecimal("2500.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();

        processPaymentRequest = ProcessPaymentRequest.builder()
                .bookingId("booking-123")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .amount(new BigDecimal("2500.00"))
                .build();



            testBookingDto = BookingDto.builder()
                    .id("booking-123")
                    .appointmentId("appt-123")
                    .requesterId(1L)
                    .providerId(2L)
                    .propertyId(100L)
                    .status(BookingStatus.PENDING)
                    .totalAmount(new BigDecimal("2500.00"))
                    .monthlyRent(new BigDecimal("1250.00"))
                    .depositAmount(new BigDecimal("1250.00"))
                    .createdAt(LocalDateTime.now())
                    .build();

            createBookingRequest = CreateBookingRequest.builder()
                    .appointmentId("appt-123")
                    .bookingDurationMonths(12)
                    .moveInDate(LocalDateTime.now().plusDays(30))
                    .moveOutDate(LocalDateTime.now().plusMonths(12).plusDays(30))
                    .build();

            testPaymentDto = PaymentDto.builder()
                    .id("payment-123")
                    .bookingId("booking-123")
                    .payerId(1L)
                    .amount(new BigDecimal("2500.00"))
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .status(PaymentStatus.COMPLETED)
                    .createdAt(LocalDateTime.now())
                    .build();

            processPaymentRequest = ProcessPaymentRequest.builder()
                    .bookingId("booking-123")
                    .paymentMethod(PaymentMethod.CREDIT_CARD)
                    .amount(new BigDecimal("2500.00"))
                    .build();


    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void createBooking_WithStudentRole_Success() throws Exception {
        // Arrange
        when(bookingService.createBooking(any(CreateBookingRequest.class))).thenReturn(testBookingDto);

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createBookingRequest)))
                .andExpect(status().isCreated());

        verify(bookingService).createBooking(any(CreateBookingRequest.class));
    }


    @Test
    @WithMockUser(roles = "STUDENT")
    void getBookingById_Success() throws Exception {
        when(bookingService.getBookingById("booking-123")).thenReturn(testBookingDto);

        mockMvc.perform(get("/api/bookings/booking-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(bookingService).getBookingById("booking-123");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllBookings_Success() throws Exception {
        List<BookingDto> bookings = Arrays.asList(testBookingDto);
        when(bookingService.getAllBookings()).thenReturn(bookings);

        mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(bookingService).getAllBookings();
    }

    @Test
    @WithMockUser(roles = "USER")
    void getBookingsByRequester_Success() throws Exception {
        List<BookingDto> bookings = Arrays.asList(testBookingDto);
        when(bookingService.getBookingsByRequesterId(1L)).thenReturn(bookings);

        mockMvc.perform(get("/api/bookings/requester/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(bookingService).getBookingsByRequesterId(1L);
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    void updateBookingStatus_Success() throws Exception {
        BookingDto updatedBooking = BookingDto.builder()
                .id("booking-123")
                .status(BookingStatus.CONFIRMED)
                .build();

        when(bookingService.updateBookingStatus("booking-123", BookingStatus.CONFIRMED))
                .thenReturn(updatedBooking);

        mockMvc.perform(put("/api/bookings/booking-123/status")
                        .param("status", "CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(bookingService).updateBookingStatus("booking-123", BookingStatus.CONFIRMED);
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void cancelBooking_Success() throws Exception {
        BookingDto cancelledBooking = BookingDto.builder()
                .id("booking-123")
                .status(BookingStatus.CANCELLED)
                .build();

        when(bookingService.cancelBooking("booking-123", "test"))
                .thenReturn(cancelledBooking);

        mockMvc.perform(put("/api/bookings/booking-123/cancel")
                        .param("reason", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(bookingService).cancelBooking("booking-123", "test");
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void processPayment_Success() throws Exception {
        // Arrange
        ProcessPaymentRequest request = new ProcessPaymentRequest();
        request.setBookingId("123");
        request.setAmount(new BigDecimal("100.00"));
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        request.setPaymentType(PaymentType.DEPOSIT);

        PaymentDto testPaymentDto = new PaymentDto();
        testPaymentDto.setStatus(PaymentStatus.PENDING);

        when(bookingService.processPayment(any(ProcessPaymentRequest.class)))
                .thenReturn(testPaymentDto);

        String jsonRequest = objectMapper.writeValueAsString(request);
        System.out.println(jsonRequest); // <--- Make sure paymentMethod is "CREDIT_CARD"

        // Act & Assert
        mockMvc.perform(post("/api/bookings/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payment processed successfully"))
                .andExpect(jsonPath("$.payment.status").value("PENDING"));

        verify(bookingService).processPayment(any(ProcessPaymentRequest.class));
    }


    @Test
    @WithMockUser
    void getPaymentByBookingId_Success() throws Exception {
        when(bookingService.getPaymentByBookingId("booking-123")).thenReturn(testPaymentDto);

        mockMvc.perform(get("/api/bookings/payments/booking/booking-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(bookingService).getPaymentByBookingId("booking-123");
    }

    @Test
    @WithMockUser
    void confirmBooking_Success() throws Exception {
        BookingDto confirmedBooking = BookingDto.builder()
                .id("booking-123")
                .status(BookingStatus.CONFIRMED)
                .build();

        when(bookingService.confirmBooking("token")).thenReturn(confirmedBooking);

        mockMvc.perform(get("/api/bookings/confirm/token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(bookingService).confirmBooking("token");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteBooking_Success() throws Exception {
        doNothing().when(bookingService).deleteBooking("booking-123");

        mockMvc.perform(delete("/api/bookings/booking-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(bookingService).deleteBooking("booking-123");
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    void getBookingsByStatus_Success() throws Exception {
        List<BookingDto> bookings = Arrays.asList(testBookingDto);
        when(bookingService.getBookingsByStatus(BookingStatus.PENDING)).thenReturn(bookings);

        mockMvc.perform(get("/api/bookings/status/PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(bookingService).getBookingsByStatus(BookingStatus.PENDING);
    }

    @Test
    @WithMockUser(roles = "LANDLORD")
    void getBookingsByProvider_Success() throws Exception {
        List<BookingDto> bookings = Arrays.asList(testBookingDto);
        when(bookingService.getBookingsByProviderId(2L)).thenReturn(bookings);

        mockMvc.perform(get("/api/bookings/provider/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(bookingService).getBookingsByProviderId(2L);
    }
}