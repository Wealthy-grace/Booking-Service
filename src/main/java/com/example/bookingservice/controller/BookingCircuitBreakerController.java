//package com.example.bookingservice.controller;
//
//
//import io.github.resilience4j.circuitbreaker.CircuitBreaker;
//import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@Slf4j
//@RestController
//@RequestMapping("/api/bookings/circuit-breaker")
//@RequiredArgsConstructor
//public class BookingCircuitBreakerController {
//
//    private final CircuitBreakerRegistry circuitBreakerRegistry;
//
//    @PreAuthorize("hasRole('ADMIN')")
//    @GetMapping("/status")
//    public ResponseEntity<Map<String, Object>> getAllStatus() {
//        log.info("📊 [BOOKING] Fetching all circuit breaker statuses");
//
//        Map<String, Object> response = circuitBreakerRegistry.getAllCircuitBreakers()
//                .stream()
//                .collect(Collectors.toMap(
//                        CircuitBreaker::getName,
//                        this::getDetails
//                ));
//
//        return ResponseEntity.ok(response);
//    }
//
//    @PreAuthorize("hasRole('ADMIN')")
//    @GetMapping("/status/{name}")
//    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable String name) {
//        try {
//            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(name);
//            return ResponseEntity.ok(getDetails(cb));
//        } catch (Exception e) {
//            return ResponseEntity.notFound().build();
//        }
//    }
//
//    @PreAuthorize("hasRole('ADMIN')")
//    @PostMapping("/reset/{name}")
//    public ResponseEntity<Map<String, String>> reset(@PathVariable String name) {
//        try {
//            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(name);
//            cb.transitionToClosedState();
//
//            Map<String, String> response = new HashMap<>();
//            response.put("message", "Circuit breaker reset");
//            response.put("name", name);
//            response.put("state", cb.getState().toString());
//
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().build();
//        }
//    }
//
//    private Map<String, Object> getDetails(CircuitBreaker cb) {
//        Map<String, Object> details = new HashMap<>();
//        CircuitBreaker.Metrics metrics = cb.getMetrics();
//
//        details.put("name", cb.getName());
//        details.put("state", cb.getState().toString());
//        details.put("failureRate", String.format("%.2f%%", metrics.getFailureRate()));
//        details.put("slowCallRate", String.format("%.2f%%", metrics.getSlowCallRate()));
//        details.put("bufferedCalls", metrics.getNumberOfBufferedCalls());
//        details.put("failedCalls", metrics.getNumberOfFailedCalls());
//        details.put("successfulCalls", metrics.getNumberOfSuccessfulCalls());
//        details.put("notPermittedCalls", metrics.getNumberOfNotPermittedCalls());
//
//        return details;
//    }
//}


// TODO - implement circuit breaker
package com.example.bookingservice.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for monitoring and managing Circuit Breakers
 * Provides endpoints for viewing circuit breaker status and resetting them
 */
@Slf4j
@RestController
@RequestMapping("/api/bookings/circuit-breaker")
@RequiredArgsConstructor
@Tag(name = "Circuit Breaker Management", description = "APIs for monitoring and managing circuit breakers")
@SecurityRequirement(name = "bearer-jwt")
public class BookingCircuitBreakerController {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    /**
     * Get status of all registered circuit breakers
     * Admin only endpoint
     *
     * @return Map of circuit breaker names to their detailed status
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/status")
    @Operation(summary = "Get all circuit breaker statuses", description = "Returns status of all registered circuit breakers (Admin only)")
    public ResponseEntity<Map<String, Object>> getAllStatus() {
        log.info("📊 [BOOKING] Fetching all circuit breaker statuses");

        Map<String, Object> response = circuitBreakerRegistry.getAllCircuitBreakers()
                .stream()
                .collect(Collectors.toMap(
                        CircuitBreaker::getName,
                        this::getDetails
                ));

        return ResponseEntity.ok(response);
    }

    /**
     * Get status of a specific circuit breaker by name
     * Admin only endpoint
     *
     * @param name Circuit breaker name
     * @return Detailed status of the circuit breaker
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/status/{name}")
    @Operation(summary = "Get circuit breaker status by name", description = "Returns detailed status of a specific circuit breaker (Admin only)")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable String name) {
        log.info("📊 [BOOKING] Fetching circuit breaker status for: {}", name);

        try {
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(name);
            return ResponseEntity.ok(getDetails(cb));
        } catch (Exception e) {
            log.error("❌ [BOOKING] Circuit breaker not found: {}", name);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Reset a circuit breaker to CLOSED state
     * Admin only endpoint
     *
     * @param name Circuit breaker name to reset
     * @return Success message with new state
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reset/{name}")
    @Operation(summary = "Reset circuit breaker", description = "Manually reset a circuit breaker to CLOSED state (Admin only)")
    public ResponseEntity<Map<String, String>> reset(@PathVariable String name) {
        log.info("🔄 [BOOKING] Resetting circuit breaker: {}", name);

        try {
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(name);
            cb.transitionToClosedState();

            Map<String, String> response = new HashMap<>();
            response.put("message", "Circuit breaker reset");
            response.put("name", name);
            response.put("state", cb.getState().toString());

            log.info("✅ [BOOKING] Circuit breaker reset successfully: {} -> {}", name, cb.getState());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ [BOOKING] Failed to reset circuit breaker: {}", name, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Build detailed status map for a circuit breaker
     *
     * @param cb Circuit breaker instance
     * @return Map containing detailed metrics and state
     */
    private Map<String, Object> getDetails(CircuitBreaker cb) {
        Map<String, Object> details = new HashMap<>();
        CircuitBreaker.Metrics metrics = cb.getMetrics();

        details.put("name", cb.getName());
        details.put("state", cb.getState().toString());
        details.put("failureRate", String.format("%.2f%%", metrics.getFailureRate()));
        details.put("slowCallRate", String.format("%.2f%%", metrics.getSlowCallRate()));
        details.put("bufferedCalls", metrics.getNumberOfBufferedCalls());
        details.put("failedCalls", metrics.getNumberOfFailedCalls());
        details.put("successfulCalls", metrics.getNumberOfSuccessfulCalls());
        details.put("notPermittedCalls", metrics.getNumberOfNotPermittedCalls());

        return details;
    }
}