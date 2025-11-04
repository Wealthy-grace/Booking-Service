package com.example.bookingservice.controller;

import com.example.bookingservice.business.interfaces.BookingService;
import com.example.bookingservice.domain.dto.BookingDto;
import com.example.bookingservice.domain.dto.PaymentDto;
import com.example.bookingservice.domain.request.CreateBookingRequest;
import com.example.bookingservice.domain.request.ProcessPaymentRequest;
import com.example.bookingservice.domain.response.BookingResponse;
import com.example.bookingservice.domain.response.BookingListResponse;
import com.example.bookingservice.domain.response.PaymentResponse;
import com.example.bookingservice.persistence.model.BookingStatus;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;


@Slf4j
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Booking Management", description = "APIs for managing property bookings")
@SecurityRequirement(name = "bearer-jwt")
public class BookingController {

    private final BookingService bookingService;


    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Create a new booking", description = "Create a booking after viewing an appointment")
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody CreateBookingRequest request
    ) {
        log.info("REST request to create booking for appointment: {}", request.getAppointmentId());
        BookingDto booking = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BookingResponse.success("Booking created successfully", booking));
    }

    @GetMapping("/{id}")
    //@PreAuthorize("isAuthenticated()")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')")
    @Operation(summary = "Get booking by ID")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable String id) {
        log.info("REST request to get booking: {}", id);
        BookingDto booking = bookingService.getBookingById(id);
        return ResponseEntity.ok(BookingResponse.success("Booking retrieved successfully", booking));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LANDLORD', 'PROPERTY_MANAGER')")
    @Operation(summary = "Get all bookings", description = "Admin/Landlord only")
    public ResponseEntity<BookingListResponse> getAllBookings() {
        log.info("REST request to get all bookings");
        List<BookingDto> bookings = bookingService.getAllBookings();
        return ResponseEntity.ok(BookingListResponse.success("Bookings retrieved successfully", bookings));
    }

    @GetMapping("/requester/{requesterId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get bookings by requester ID")
    public ResponseEntity<BookingListResponse> getBookingsByRequester(@PathVariable Long requesterId) {
        log.info("REST request to get bookings for requester: {}", requesterId);
        List<BookingDto> bookings = bookingService.getBookingsByRequesterId(requesterId);
        return ResponseEntity.ok(BookingListResponse.success("Bookings retrieved successfully", bookings));
    }

    @GetMapping("/provider/{providerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LANDLORD')")
    @Operation(summary = "Get bookings by provider ID")
    public ResponseEntity<BookingListResponse> getBookingsByProvider(@PathVariable Long providerId) {
        log.info("REST request to get bookings for provider: {}", providerId);
        List<BookingDto> bookings = bookingService.getBookingsByProviderId(providerId);
        return ResponseEntity.ok(BookingListResponse.success("Bookings retrieved successfully", bookings));
    }

    @GetMapping("/property/{propertyId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get bookings by property ID")
    public ResponseEntity<BookingListResponse> getBookingsByProperty(@PathVariable Long propertyId) {
        log.info("REST request to get bookings for property: {}", propertyId);
        List<BookingDto> bookings = bookingService.getBookingsByPropertyId(propertyId);
        return ResponseEntity.ok(BookingListResponse.success("Bookings retrieved successfully", bookings));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LANDLORD')")
    @Operation(summary = "Get bookings by status")
    public ResponseEntity<BookingListResponse> getBookingsByStatus(@PathVariable BookingStatus status) {
        log.info("REST request to get bookings with status: {}", status);
        List<BookingDto> bookings = bookingService.getBookingsByStatus(status);
        return ResponseEntity.ok(BookingListResponse.success("Bookings retrieved successfully", bookings));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'LANDLORD')")
    @Operation(summary = "Update booking status")
    public ResponseEntity<BookingResponse> updateBookingStatus(
            @PathVariable String id,
            @RequestParam BookingStatus status
    ) {
        log.info("REST request to update booking {} status to {}", id, status);
        BookingDto booking = bookingService.updateBookingStatus(id, status);
        return ResponseEntity.ok(BookingResponse.success("Booking status updated successfully", booking));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancel booking")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable String id,
            @RequestParam(required = false) String reason
    ) {
        log.info("REST request to cancel booking: {}", id);
        BookingDto booking = bookingService.cancelBooking(id, reason);
        return ResponseEntity.ok(BookingResponse.success("Booking cancelled successfully", booking));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete booking", description = "Admin only")
    public ResponseEntity<BookingResponse> deleteBooking(@PathVariable String id) {
        log.info("REST request to delete booking: {}", id);
        bookingService.deleteBooking(id);
        return ResponseEntity.ok(BookingResponse.success("Booking deleted successfully", null));
    }


    @PostMapping("/payments")
    @PreAuthorize("hasAnyRole('STUDENT', 'TENANT')")
    @Operation(summary = "Process payment for booking")
    public ResponseEntity<PaymentResponse> processPayment(
            @Valid @RequestBody ProcessPaymentRequest request
    ) {
        log.info("REST request to process payment for booking: {}", request.getBookingId());
        PaymentDto payment = bookingService.processPayment(request);
        return ResponseEntity.ok(PaymentResponse.success("Payment processed successfully", payment));
    }

    @GetMapping("/payments/booking/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get payment by booking ID")
    public ResponseEntity<PaymentResponse> getPaymentByBookingId(@PathVariable String bookingId) {
        log.info("REST request to get payment for booking: {}", bookingId);
        PaymentDto payment = bookingService.getPaymentByBookingId(bookingId);
        return ResponseEntity.ok(PaymentResponse.success("Payment retrieved successfully", payment));
    }

    @GetMapping("/payments/payer/{payerId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get payments by payer ID")
    public ResponseEntity<PaymentResponse> getPaymentsByPayerId(@PathVariable Long payerId) {
        log.info("REST request to get payments for payer: {}", payerId);
        List<PaymentDto> payments = bookingService.getPaymentsByPayerId(payerId);
        return ResponseEntity.ok(PaymentResponse.successList("Payments retrieved successfully", payments));
    }



    @GetMapping("/confirm/{token}")
    @Operation(summary = "Confirm booking with token")
    public ResponseEntity<BookingResponse> confirmBooking(@PathVariable String token) {
        log.info("REST request to confirm booking with token");
        BookingDto booking = bookingService.confirmBooking(token);
        return ResponseEntity.ok(BookingResponse.success("Booking confirmed successfully", booking));
    }

    @PutMapping("/property/{propertyId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LANDLORD')")
    @Operation(summary = "Update property status if property is rented")
    public ResponseEntity<BookingResponse> updatePropertyStatus(
            @PathVariable String propertyId,
            @RequestBody BookingDto request) {

        log.info("REST request to update property status if property is rented: {}", propertyId);

        BookingDto booking = bookingService.updatePropertyStatus(propertyId, request.getPropertyIsRented());

        return ResponseEntity.ok(
                BookingResponse.success("Property status updated successfully", booking)
        );
    }
}