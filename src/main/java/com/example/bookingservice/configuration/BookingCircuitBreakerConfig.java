package com.example.bookingservice.configuration;


import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class BookingCircuitBreakerConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(defaultConfig);

        // User Service CB
        CircuitBreaker userServiceCB = registry.circuitBreaker("userService");
        userServiceCB.getEventPublisher()
                .onStateTransition(event ->
                        log.warn("🔄 [BOOKING] USER SERVICE CB: {} -> {}",
                                event.getStateTransition().getFromState(),
                                event.getStateTransition().getToState()))
                .onError(event ->
                        log.error("❌ [BOOKING] USER SERVICE CB ERROR: {}",
                                event.getThrowable().getMessage()))
                .onCallNotPermitted(event ->
                        log.warn("🚫 [BOOKING] USER SERVICE CB: Call blocked (Circuit OPEN)"));

        // Property Service CB
        CircuitBreaker propertyServiceCB = registry.circuitBreaker("propertyService");
        propertyServiceCB.getEventPublisher()
                .onStateTransition(event ->
                        log.warn("🔄 [BOOKING] PROPERTY SERVICE CB: {} -> {}",
                                event.getStateTransition().getFromState(),
                                event.getStateTransition().getToState()))
                .onError(event ->
                        log.error("❌ [BOOKING] PROPERTY SERVICE CB ERROR: {}",
                                event.getThrowable().getMessage()));

        // Appointment Service CB
        CircuitBreaker appointmentServiceCB = registry.circuitBreaker("appointmentService");
        appointmentServiceCB.getEventPublisher()
                .onStateTransition(event ->
                        log.warn("🔄 [BOOKING] APPOINTMENT SERVICE CB: {} -> {}",
                                event.getStateTransition().getFromState(),
                                event.getStateTransition().getToState()))
                .onError(event ->
                        log.error("❌ [BOOKING] APPOINTMENT SERVICE CB ERROR: {}",
                                event.getThrowable().getMessage()));

        return registry;
    }
}