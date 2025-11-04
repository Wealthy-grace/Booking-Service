package com.example.bookingservice.controller;

import com.example.bookingservice.event.BookingEvent;
import com.example.bookingservice.producer.BookingEventProducer;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

 // Test controller to verify RabbitMQ integration

@Slf4j
@RestController
@RequestMapping("/api/bookings/test/rabbitmq")
@RequiredArgsConstructor
@Tag(name = "RabbitMQ Testing", description = "Test endpoints for RabbitMQ integration")
public class RabbitMQTestController {

    private final BookingEventProducer bookingEventProducer;


    // Test endpoint to send a booking event to RabbitMQ
    @PostMapping("/send-event")
    public ResponseEntity<Map<String, Object>> sendTestBookingEvent() {
        log.info("Sending test booking event to RabbitMQ...");

        try {
            // Create a test booking event
            BookingEvent testEvent = BookingEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("BOOKING_CREATED")
                    .eventTimestamp(LocalDateTime.now())
                    .bookingId("test-booking-" + System.currentTimeMillis())
                    .appointmentId("test-appt-123")
                    .status("PENDING")
                    .propertyId(1L)
                    .propertyTitle("Test Property - Luxury Apartment")
                    .propertyAddress("123 Test Street, Amsterdam")
                    .requesterId(100L)
                    .requesterName("John Doe")
                    .requesterEmail("john.doe@example.com")
                    .providerId(200L)
                    .providerName("Jane Smith")
                    .providerEmail("jane.smith@example.com")
                    .totalAmount(new BigDecimal("2500.00"))
                    .monthlyRent(new BigDecimal("1250.00"))
                    .depositAmount(new BigDecimal("1250.00"))
                    .bookingDurationMonths(12)
                    .moveInDate(LocalDateTime.now().plusDays(30))
                    .moveOutDate(LocalDateTime.now().plusYears(1).plusDays(30))
                    .paymentStatus("PENDING")
                    .build();

            // Publish the event
            bookingEventProducer.publishBookingCreated(testEvent);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Test booking event sent successfully!");
            response.put("eventId", testEvent.getEventId());
            response.put("eventType", testEvent.getEventType());
            response.put("bookingId", testEvent.getBookingId());
            response.put("timestamp", testEvent.getEventTimestamp());
            response.put("instructions", "Check RabbitMQ UI -> Queues -> booking-queue to see the message");

            log.info("✓ Test event sent successfully: {}", testEvent.getEventId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to send test event", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("message", "Failed to send test event. Check logs for details.");

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }


     //Health check endpoint for RabbitMQ

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkRabbitMQHealth() {
        Map<String, Object> health = new HashMap<>();

        try {
            // If we can inject the producer, connection is likely working
            health.put("status", "UP");
            health.put("message", "RabbitMQ connection is healthy");
            health.put("producerInitialized", bookingEventProducer != null);
            health.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            health.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(503).body(health);
        }
    }

     /// Get RabbitMQ configuration info

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getRabbitMQConfig() {
        Map<String, Object> config = new HashMap<>();

        config.put("exchange", "app-exchange");
        config.put("bookingQueue", "booking-queue");
        config.put("appointmentQueue", "appointment-queue");
        config.put("bookingRoutingKey", "booking.key");
        config.put("appointmentRoutingKey", "appointment.key");
        config.put("managementUI", "http://localhost:15672");
        config.put("credentials", "guest/guest");

        Map<String, String> instructions = new HashMap<>();
        instructions.put("step1", "Access RabbitMQ Management UI: http://localhost:15672");
        instructions.put("step2", "Login with guest/guest");
        instructions.put("step3", "Go to Exchanges tab to see 'app-exchange'");
        instructions.put("step4", "Go to Queues tab to see 'booking-queue' and 'appointment-queue'");
        instructions.put("step5", "Click on 'app-exchange' to see bindings");

        config.put("instructions", instructions);

        return ResponseEntity.ok(config);
    }

     //-
    // +-end multiple test events

    @PostMapping("/send-multiple")
    public ResponseEntity<Map<String, Object>> sendMultipleTestEvents(
            @RequestParam(defaultValue = "5") int count) {

        log.info("Sending {} test booking events to RabbitMQ...", count);

        Map<String, Object> response = new HashMap<>();
        int successCount = 0;
        int failureCount = 0;

        for (int i = 0; i < count; i++) {
            try {
                BookingEvent testEvent = BookingEvent.builder()
                        .eventId(UUID.randomUUID().toString())
                        .eventType("BOOKING_CREATED")
                        .eventTimestamp(LocalDateTime.now())
                        .bookingId("test-booking-" + i + "-" + System.currentTimeMillis())
                        .appointmentId("test-appt-" + i)
                        .status("PENDING")
                        .propertyId((long) (i + 1))
                        .propertyTitle("Test Property " + (i + 1))
                        .totalAmount(new BigDecimal("2500.00"))
                        .monthlyRent(new BigDecimal("1250.00"))
                        .build();

                bookingEventProducer.publishBookingCreated(testEvent);
                successCount++;

            } catch (Exception e) {
                log.error("Failed to send event {}: {}", i, e.getMessage());
                failureCount++;
            }
        }

        response.put("success", failureCount == 0);
        response.put("totalRequested", count);
        response.put("successCount", successCount);
        response.put("failureCount", failureCount);
        response.put("message", String.format("Sent %d/%d events successfully", successCount, count));
        response.put("timestamp", LocalDateTime.now());

        log.info("✓ Sent {}/{} test events successfully", successCount, count);

        return ResponseEntity.ok(response);
    }
}