package com.example.bookingservice.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



// RabbitMQ Configuration for Booking Service
// * Creates exchanges, queues, and bindings for event-driven communication
@Slf4j
@Configuration
public class RabbitMQConfig {

    // Exchange and Queue Names
    public static final String EXCHANGE_NAME = "app-exchange";
    public static final String APPOINTMENT_QUEUE = "appointment-queue";
    public static final String BOOKING_QUEUE = "booking-queue";

    // Routing Keys
    public static final String APPOINTMENT_ROUTING_KEY = "appointment.key";
    public static final String BOOKING_ROUTING_KEY = "booking.key";

    /**
     * Create the main application exchange (Direct Exchange)
     * Durable = survives broker restart
     */
    @Bean
    public DirectExchange appExchange() {
        log.info("Creating DirectExchange: {}", EXCHANGE_NAME);
        return ExchangeBuilder
                .directExchange(EXCHANGE_NAME)
                .durable(true)
                .build();
    }

    /**
     * Queue for appointment events (consumed by Booking Service)
     */
    @Bean
    public Queue appointmentQueue() {
        log.info("Creating Queue: {}", APPOINTMENT_QUEUE);
        return QueueBuilder
                .durable(APPOINTMENT_QUEUE)
                .build();
    }

    /**
     * Queue for booking events (consumed by other services)
     */
    @Bean
    public Queue bookingQueue() {
        log.info("Creating Queue: {}", BOOKING_QUEUE);
        return QueueBuilder
                .durable(BOOKING_QUEUE)
                .build();
    }


    // Bind appointment queue to exchange with appointment routing key
    @Bean
    public Binding appointmentBinding(Queue appointmentQueue, DirectExchange appExchange) {
        log.info("Binding {} to {} with routing key: {}",
                APPOINTMENT_QUEUE, EXCHANGE_NAME, APPOINTMENT_ROUTING_KEY);
        return BindingBuilder
                .bind(appointmentQueue)
                .to(appExchange)
                .with(APPOINTMENT_ROUTING_KEY);
    }

   // Bind booking queue to exchange with booking routing key
    @Bean
    public Binding bookingBinding(Queue bookingQueue, DirectExchange appExchange) {
        log.info("Binding {} to {} with routing key: {}",
                BOOKING_QUEUE, EXCHANGE_NAME, BOOKING_ROUTING_KEY);
        return BindingBuilder
                .bind(bookingQueue)
                .to(appExchange)
                .with(BOOKING_ROUTING_KEY);
    }

    /**
     * JSON message converter for serializing/deserializing events
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        log.info("Configuring Jackson2JsonMessageConverter");
        return new Jackson2JsonMessageConverter();
    }



    // RabbitMQ Admin for programmatic management
    @Bean
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        log.info("Creating RabbitAdmin");
        return new RabbitAdmin(connectionFactory);
    }

    /**
     * RabbitTemplate for sending messages
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        log.info("Configuring RabbitTemplate");
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    //  Initialize RabbitMQ infrastructure on application startup
    // This ensures all exchanges, queues, and bindings are created
    @Bean
    public CommandLineRunner initRabbitMQ(RabbitAdmin rabbitAdmin,
                                          DirectExchange exchange,
                                          Queue appointmentQueue,
                                          Queue bookingQueue,
                                          Binding appointmentBinding,
                                          Binding bookingBinding) {
        return args -> {
            log.info("========================================");
            log.info("Initializing RabbitMQ Infrastructure...");
            log.info("========================================");

            try {
                // Declare exchange
                rabbitAdmin.declareExchange(exchange);
                log.info("✓ Exchange declared: {}", EXCHANGE_NAME);

                // Declare queues
                rabbitAdmin.declareQueue(appointmentQueue);
                log.info("✓ Queue declared: {}", APPOINTMENT_QUEUE);

                rabbitAdmin.declareQueue(bookingQueue);
                log.info("✓ Queue declared: {}", BOOKING_QUEUE);

                // Declare bindings
                rabbitAdmin.declareBinding(appointmentBinding);
                log.info("✓ Binding created: {} -> {} [{}]",
                        APPOINTMENT_QUEUE, EXCHANGE_NAME, APPOINTMENT_ROUTING_KEY);

                rabbitAdmin.declareBinding(bookingBinding);
                log.info("✓ Binding created: {} -> {} [{}]",
                        BOOKING_QUEUE, EXCHANGE_NAME, BOOKING_ROUTING_KEY);

                log.info("========================================");
                log.info("RabbitMQ Infrastructure Ready!");
                log.info("Management UI: http://localhost:15672");
                log.info("========================================");

            } catch (Exception e) {
                log.error("Failed to initialize RabbitMQ infrastructure", e);
                log.error("Please check:");
                log.error("  1. RabbitMQ is running (docker ps | grep rabbitmq)");
                log.error("  2. Connection settings in application.properties");
                log.error("  3. Credentials are correct (default: guest/guest)");
                throw e;
            }
        };
    }
}