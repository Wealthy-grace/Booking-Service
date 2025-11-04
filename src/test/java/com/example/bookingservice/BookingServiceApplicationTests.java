package com.example.bookingservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.utility.TestcontainersConfiguration;

/**
 * Integration test for BookingServiceApplication
 * Tests application context loading with Testcontainers
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class BookingServiceApplicationTests {

    @Test
    void contextLoads() {
        // This test verifies that the Spring application context loads successfully
        // with all necessary beans and configurations
    }
}