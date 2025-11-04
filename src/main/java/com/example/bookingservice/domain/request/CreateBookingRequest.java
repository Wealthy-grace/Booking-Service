package com.example.bookingservice.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {

    @NotBlank(message = "Appointment ID is required")
    private String appointmentId;

    @NotNull(message = "Move-in date is required")
    private LocalDateTime moveInDate;

    @NotNull(message = "Move-out date is required")
    private LocalDateTime moveOutDate;

    @NotNull(message = "Booking duration is required")
    @Positive(message = "Duration must be positive")
    private Integer bookingDurationMonths;

    private String notes;
}
























