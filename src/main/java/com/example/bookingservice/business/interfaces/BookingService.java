package com.example.bookingservice.business.interfaces;

import com.example.bookingservice.domain.dto.BookingDto;
import com.example.bookingservice.domain.dto.PaymentDto;
import com.example.bookingservice.domain.request.CreateBookingRequest;
import com.example.bookingservice.domain.request.ProcessPaymentRequest;
import com.example.bookingservice.persistence.model.BookingStatus;

import java.util.List;

public interface BookingService {

    BookingDto createBooking(CreateBookingRequest request);


    BookingDto getBookingById(String id);


    List<BookingDto> getAllBookings();


    List<BookingDto> getBookingsByRequesterId(Long requesterId);


    List<BookingDto> getBookingsByProviderId(Long providerId);


    List<BookingDto> getBookingsByPropertyId(Long propertyId);


    List<BookingDto> getBookingsByStatus(BookingStatus status);


    BookingDto updateBookingStatus(String id, BookingStatus status);


    BookingDto cancelBooking(String id, String reason);


    void deleteBooking(String id);


    BookingDto confirmBooking(String confirmationToken);


    PaymentDto processPayment(ProcessPaymentRequest request);


    PaymentDto getPaymentByBookingId(String bookingId);

    List<BookingDto>   getBookingsByAppointmentId(String appointmentId);

    // Update Property status if Property is Rented
    BookingDto updatePropertyStatus(String propertyId, boolean propertyIsRented);



    List<PaymentDto> getPaymentsByPayerId(Long payerId);
}