//package com.example.bookingservice.configuration;
//
//import com.example.bookingservice.client.AppointmentServiceClient;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.client.JdkClientHttpRequestFactory;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.oauth2.jwt.Jwt;
//import org.springframework.web.client.RestClient;
//import org.springframework.web.client.support.RestClientAdapter;
//import org.springframework.web.service.invoker.HttpServiceProxyFactory;
//
//
//// Rest Client Configuration for Booking Service
////  Creates HTTP Interface clients with automatic JWT to
//@Slf4j
//@Configuration
//public class BookingRestClientConfig {
//
//    @Value("${app.services.appointment-service.url:http://localhost:8083}")
//    private String appointmentServiceUrl;
//
//    // Create RestClient with JWT token forwarding interceptor
//    private RestClient createRestClientWithAuth(String baseUrl) {
//        return RestClient.builder()
//                .baseUrl(baseUrl)
//                .requestFactory(new JdkClientHttpRequestFactory())
//                .requestInterceptor((request, body, execution) -> {
//                    // Extract JWT token from SecurityContext
//                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//
//                    if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
//                        Jwt jwt = (Jwt) authentication.getPrincipal();
//
//                        // Add Authorization header with Bearer token
//                        request.getHeaders().setBearerAuth(jwt.getTokenValue());
//
//                        log.debug("Added JWT token to request: {} {}",
//                                request.getMethod(),
//                                request.getURI());
//                    } else {
//                        log.warn("No JWT token found in SecurityContext for request: {} {}",
//                                request.getMethod(),
//                                request.getURI());
//                    }
//
//                    return execution.execute(request, body);
//                })
//                .build();
//    }
//
//
//    // Appointment Service Client Bean
//    @Bean
//    public AppointmentServiceClient appointmentServiceClient() {
//        log.info("Creating AppointmentServiceClient with base URL: {}", appointmentServiceUrl);
//
//        // Create RestClient with JWT forwarding
//        RestClient restClient = createRestClientWithAuth(appointmentServiceUrl);
//
//        // Create adapter for HTTP Interface
//        RestClientAdapter adapter = RestClientAdapter.create(restClient);
//
//        // Create proxy factory
//        HttpServiceProxyFactory factory = HttpServiceProxyFactory
//                .builderFor(adapter)
//                .build();
//
//        // Create and return the client proxy
//        return factory.createClient(AppointmentServiceClient.class);
//    }
//}


// TODO : Add Configuration for Property Service

package com.example.bookingservice.configuration;

import com.example.bookingservice.client.AppointmentServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Slf4j
@Configuration
public class BookingRestClientConfig {

    // ⚠️ FIXED: Changed property path to match docker-compose.yml
    @Value("${microservices.appointment-service.url:http://localhost:8083}")
    private String appointmentServiceUrl;

    /**
     * Create RestClient with JWT token forwarding interceptor
     */
    private RestClient createRestClientWithAuth(String baseUrl) {
        log.info("🔧 Creating RestClient with base URL: {}", baseUrl);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new JdkClientHttpRequestFactory())
                .requestInterceptor((request, body, execution) -> {
                    // Extract JWT token from SecurityContext
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                    if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
                        // Add Authorization header with Bearer token
                        request.getHeaders().setBearerAuth(jwt.getTokenValue());

                        log.debug("✅ Added JWT token to request: {} {}",
                                request.getMethod(),
                                request.getURI());
                    } else {
                        log.warn("⚠️ No JWT token found in SecurityContext for request: {} {}",
                                request.getMethod(),
                                request.getURI());
                    }

                    return execution.execute(request, body);
                })
                .build();
    }

    /**
     * Appointment Service Client Bean
     */
    @Bean
    public AppointmentServiceClient appointmentServiceClient() {
        log.info("🚀 Creating AppointmentServiceClient with base URL: {}", appointmentServiceUrl);

        // Create RestClient with JWT forwarding
        RestClient restClient = createRestClientWithAuth(appointmentServiceUrl);

        // Create adapter for HTTP Interface
        RestClientAdapter adapter = RestClientAdapter.create(restClient);

        // Create proxy factory
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(adapter)
                .build();

        // Create and return the client proxy
        return factory.createClient(AppointmentServiceClient.class);
    }
}