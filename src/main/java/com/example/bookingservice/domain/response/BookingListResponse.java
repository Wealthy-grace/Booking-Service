package com.example.bookingservice.domain.response;


import com.example.bookingservice.domain.dto.BookingDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Booking List Response
 * Response wrapper for multiple booking operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingListResponse {

    private Boolean success;
    private String message;
    private List<BookingDto> bookings;
    private Integer count;
    private String errorCode;
    private LocalDateTime timestamp;

    /**
     * Create success response with booking list
     */
    public static BookingListResponse success(String message, List<BookingDto> bookings) {
        return BookingListResponse.builder()
                .success(true)
                .message(message)
                .bookings(bookings)
                .count(bookings != null ? bookings.size() : 0)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create error response
     */
    public static BookingListResponse error(String message, String errorCode) {
        return BookingListResponse.builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create error response without error code
     */
    public static BookingListResponse error(String message) {
        return error(message, null);
    }
}