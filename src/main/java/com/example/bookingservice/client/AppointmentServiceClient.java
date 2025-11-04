package com.example.bookingservice.client;

import com.example.bookingservice.domain.response.AppointmentResponse;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;


@HttpExchange
public interface AppointmentServiceClient {

    @GetExchange("/api/v1/appointments/{id}")

    AppointmentResponse getAppointmentById(@PathVariable("id") String appointmentId);
}






















