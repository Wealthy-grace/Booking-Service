package com.example.bookingservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BookingServiceApplicationTests {

    //@Test
    void contextLoads() {
        // This test verifies that the Spring application context loads successfully
        // Embedded MongoDB will start automatically in the background
    }
}