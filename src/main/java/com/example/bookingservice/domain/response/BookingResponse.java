package com.example.bookingservice.domain.response;

import com.example.bookingservice.domain.dto.BookingDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Booking Response
 * Response wrapper for single booking operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private Boolean success;
    private String message;
    private BookingDto booking;
    private String errorCode;
    private LocalDateTime timestamp;

    /**
     * Create success response
     */
    public static BookingResponse success(String message, BookingDto booking) {
        return BookingResponse.builder()
                .success(true)
                .message(message)
                .booking(booking)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create error response
     */
    public static BookingResponse error(String message, String errorCode) {
        return BookingResponse.builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create error response without error code
     */
    public static BookingResponse error(String message) {
        return error(message, null);
    }
}