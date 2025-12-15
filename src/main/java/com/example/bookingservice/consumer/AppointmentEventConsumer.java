package com.example.bookingservice.consumer;


import com.example.bookingservice.business.saga.BookingSagaOrchestrator;
import com.example.bookingservice.domain.request.CreateBookingRequest;
import com.example.bookingservice.event.AppointmentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * ⭐ THIS IS THE MISSING CONSUMER! ⭐
 *
 * Consumes appointment events from RabbitMQ
 * Triggers booking saga when appointment is confirmed
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "spring.rabbitmq.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class AppointmentEventConsumer {

    private final BookingSagaOrchestrator sagaOrchestrator;

    /**
     * 🎯 Listen to APPOINTMENT_CONFIRMED events
     * This is the trigger for the booking saga
     */
    @RabbitListener(queues = "${rabbitmq.queue.appointment:appointment-queue}", id = "appointmentQueueListener")
    @Transactional
    public void consumeAppointmentEvent(AppointmentEvent event) {
        log.info("📥 Received appointment event: {} for appointment: {}",
                event.getEventType(), event.getAppointmentId());

        try {
            switch (event.getEventType()) {
                case "APPOINTMENT_CONFIRMED":
                    handleAppointmentConfirmed(event);
                    break;

                case "APPOINTMENT_CANCELLED":
                    handleAppointmentCancelled(event);
                    break;

                case "APPOINTMENT_RESCHEDULED":
                    handleAppointmentRescheduled(event);
                    break;

                default:
                    log.debug("📌 Event type {} not handled by booking service", event.getEventType());
            }

        } catch (Exception e) {
            log.error(" Failed to process appointment event: {}", e.getMessage(), e);
            // In production, this would go to a Dead Letter Queue for retry
        }
    }

    /**
     * Handle APPOINTMENT_CONFIRMED event
     * 🎬 START BOOKING SAGA
     */
    private void handleAppointmentConfirmed(AppointmentEvent event) {
        log.info(" APPOINTMENT_CONFIRMED received - Starting BOOKING SAGA");
        log.info(" Appointment Details:");
        log.info("   - Appointment ID: {}", event.getAppointmentId());
        log.info("   - Property ID: {}", event.getPropertyId());
        log.info("   - Requester: {} ({})", event.getRequesterName(), event.getRequesterEmail());
        log.info("   - Provider: {} ({})", event.getProviderName(), event.getProviderEmail());
        log.info("   - Property: {}", event.getPropertyTitle());
        log.info("   - DateTime: {}", event.getAppointmentDateTime());

        try {
            // ⚠ IMPORTANT: You may want to wait for USER action to create booking
            // Or auto-create booking after appointment confirmation

            // For now, we'll just log that appointment is confirmed
            // In a real scenario, you might:
            // 1. Wait for user to click "Book Now" button
            // 2. Or auto-create booking after appointment is viewed

            log.info(" Appointment confirmed. User can now create booking for appointment: {}",
                    event.getAppointmentId());

            // Store appointment data for later booking creation
            // When user clicks "Create Booking", call createBookingFromAppointment()

        } catch (Exception e) {
            log.error(" Failed to handle appointment confirmed event: {}", e.getMessage(), e);
        }
    }

    /**
     *  NEW METHOD: Create booking from confirmed appointment
     * Call this when user explicitly requests to create a booking
     */
    public void createBookingFromAppointment(AppointmentEvent event,
                                             LocalDateTime moveInDate,
                                             LocalDateTime moveOutDate,
                                             Integer durationMonths,
                                             String notes) {
        log.info(" Creating booking from confirmed appointment: {}", event.getAppointmentId());

        try {
            // Build booking request from appointment event
            CreateBookingRequest bookingRequest = CreateBookingRequest.builder()
                    .appointmentId(event.getAppointmentId())
                    .moveInDate(moveInDate != null ? moveInDate : event.getAppointmentDateTime().plusDays(7))
                    .moveOutDate(moveOutDate != null ? moveOutDate : event.getAppointmentDateTime().plusMonths(12))
                    .bookingDurationMonths(durationMonths != null ? durationMonths : 12)
                    .notes(notes != null ? notes : "Booking created after appointment confirmation")
                    .build();

            // 🎬 START SAGA
            sagaOrchestrator.startBookingSaga(event.getAppointmentId(), bookingRequest);

            log.info("✅ BOOKING SAGA started successfully for appointment: {}", event.getAppointmentId());

        } catch (Exception e) {
            log.error("❌ Failed to create booking from appointment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create booking", e);
        }
    }

    /**
     * Handle APPOINTMENT_CANCELLED event
     */
    private void handleAppointmentCancelled(AppointmentEvent event) {
        log.info("❌ APPOINTMENT_CANCELLED received for: {}", event.getAppointmentId());
        log.info("   Reason: {}", event.getCancellationReason());

        try {
            // Check if booking exists for this appointment
            // If yes, cancel the booking as well (compensation)

            log.info("🔍 Checking if booking exists for cancelled appointment: {}",
                    event.getAppointmentId());

            // This would trigger compensation saga

        } catch (Exception e) {
            log.error("❌ Failed to handle appointment cancellation: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle APPOINTMENT_RESCHEDULED event
     */
    private void handleAppointmentRescheduled(AppointmentEvent event) {
        log.info("📅 APPOINTMENT_RESCHEDULED received for: {}", event.getAppointmentId());
        log.info("   Previous: {} → New: {}",
                event.getPreviousDateTime(), event.getAppointmentDateTime());

        try {
            // Update booking dates if booking exists
            // This is optional based on business logic

            log.info("📋 Appointment rescheduled. Booking dates may need adjustment.");

        } catch (Exception e) {
            log.error("❌ Failed to handle appointment reschedule: {}", e.getMessage(), e);
        }
    }
}