package com.example.bookingservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.test.context.ActiveProfiles;

//@EnableMongoAuditing
@SpringBootTest
@ActiveProfiles("test")

public class BookingServiceApplicationTests {


    @Test
    void contextLoads() {
        // This test ensures that the Spring context loads successfully
    }
}
