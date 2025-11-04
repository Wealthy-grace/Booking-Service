////package com.example.bookingservice.configuration;
////
////
////import feign.RequestInterceptor;
////import feign.RequestTemplate;
////import lombok.extern.slf4j.Slf4j;
////import org.springframework.context.annotation.Bean;
////import org.springframework.context.annotation.Configuration;
////import org.springframework.security.core.Authentication;
////import org.springframework.security.core.context.SecurityContextHolder;
////import org.springframework.security.oauth2.jwt.Jwt;
////
////@Slf4j
////@Configuration
////public class FeignConfig {
////
////    @Bean
////    public RequestInterceptor requestInterceptor() {
////        return new RequestInterceptor() {
////            @Override
////            public void apply(RequestTemplate template) {
////                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
////                if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
////                    Jwt jwt = (Jwt) authentication.getPrincipal();
////                    template.header("Authorization", "Bearer " + jwt.getTokenValue());
////                    log.debug("Added JWT token to Feign request");
////                }
////            }
////        };
////    }
////}
//
//// TO do
//
//package com.example.bookingservice.configuration;
//
//import feign.Logger;
//import feign.Request;
//import feign.RequestInterceptor;
//import feign.Retryer;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.oauth2.jwt.Jwt;
//import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
//
//import java.util.concurrent.TimeUnit;
//
///**
// * Feign Configuration with JWT Token Propagation
// *
// * CRITICAL: When appointment-service calls user-service or property-service,
// * it needs to pass the JWT token in the Authorization header.
// *
// * This interceptor automatically adds the Bearer token to all Feign requests.
// */
//@Configuration
//@Slf4j
//public class FeignConfig {
//
//    @Bean
//    Logger.Level feignLoggerLevel() {
//        return Logger.Level.BASIC;
//    }
//
//    @Bean
//    public Request.Options requestOptions() {
//        return new Request.Options(5000, TimeUnit.MILLISECONDS, 10000, TimeUnit.MILLISECONDS, true);
//    }
//
//    @Bean
//    public Retryer retryer() {
//        return new Retryer.Default(1000, 2000, 3);
//    }
//
//    /**
//     * Request Interceptor to propagate JWT token to downstream services
//     *
//     * Extracts the JWT token from SecurityContext and adds it to
//     * the Authorization header of all Feign requests.
//     */
//    @Bean
//    public RequestInterceptor requestTokenBearerInterceptor() {
//        return requestTemplate -> {
//            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//
//            if (authentication instanceof JwtAuthenticationToken) {
//                JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
//                Jwt jwt = jwtAuth.getToken();
//                String tokenValue = jwt.getTokenValue();
//
//                log.debug("Propagating JWT token to Feign client: {}...",
//                        tokenValue.substring(0, Math.min(20, tokenValue.length())));
//
//                requestTemplate.header("Authorization", "Bearer " + tokenValue);
//            } else {
//                log.debug("No JWT token found in SecurityContext for Feign request");
//            }
//        };
//    }
//}
//
//
//
//
//
//
//
//
//
//
//
//
