package com.example.bookingservice.producer;

import com.example.bookingservice.configuration.RabbitMQConfig;
import com.example.bookingservice.event.BookingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Producer for sending booking events to RabbitMQ
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "spring.rabbitmq.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class BookingEventProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publishes booking event to the exchange
     */
    public void publishBookingEvent(BookingEvent event) {
        try {
            // Set event metadata if not already set
            if (event.getEventId() == null) {
                event.setEventId(UUID.randomUUID().toString());
            }
            if (event.getEventTimestamp() == null) {
                event.setEventTimestamp(LocalDateTime.now());
            }

            log.info("Publishing booking event: {} for booking ID: {}",
                    event.getEventType(), event.getBookingId());

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.BOOKING_ROUTING_KEY,
                    event
            );

            log.info("Successfully published booking event: {}", event.getEventType());
        } catch (Exception e) {
            log.error("Failed to publish booking event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish event to RabbitMQ", e);
        }
    }

    /**
     * Convenience method for booking creation events
     */
    public void publishBookingCreated(BookingEvent event) {
        event.setEventType("BOOKING_CREATED");
        publishBookingEvent(event);
    }

    /**
     * Convenience method for booking confirmation events
     */
    public void publishBookingConfirmed(BookingEvent event) {
        event.setEventType("BOOKING_CONFIRMED");
        publishBookingEvent(event);
    }

    /**
     * Convenience method for booking cancellation events
     */
    public void publishBookingCancelled(BookingEvent event) {
        event.setEventType("BOOKING_CANCELLED");
        publishBookingEvent(event);
    }

    /**
     * Convenience method for booking payment completed events
     */
    public void publishBookingPaymentCompleted(BookingEvent event) {
        event.setEventType("BOOKING_PAYMENT_COMPLETED");
        publishBookingEvent(event);
    }

    /**
     * Convenience method for booking expired events
     */
    public void publishBookingExpired(BookingEvent event) {
        event.setEventType("BOOKING_EXPIRED");
        publishBookingEvent(event);
    }

    /**
     * Convenience method for booking completed events
     */
    public void publishBookingCompleted(BookingEvent event) {
        event.setEventType("BOOKING_COMPLETED");
        publishBookingEvent(event);
    }
}