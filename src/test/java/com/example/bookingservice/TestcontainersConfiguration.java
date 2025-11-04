//package com.example.bookingservice;
//
//import org.springframework.boot.test.context.TestConfiguration;
//import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
//import org.springframework.context.annotation.Bean;
//import org.testcontainers.containers.MongoDBContainer;
//import org.testcontainers.containers.RabbitMQContainer;
//import org.testcontainers.utility.DockerImageName;
//
///**
// * Testcontainers configuration for integration tests
// * Provides MongoDB and RabbitMQ containers for testing
// */
//@TestConfiguration(proxyBeanMethods = false)
//public class TestcontainersConfiguration {
//
//    @Bean
//    @ServiceConnection
//    MongoDBContainer mongoDBContainer() {
//        return new MongoDBContainer(DockerImageName.parse("mongo:7.0"))
//                .withExposedPorts(27017);
//    }
//
//    @Bean
//    @ServiceConnection
//    RabbitMQContainer rabbitMQContainer() {
//        return new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management"))
//                .withExposedPorts(5672, 15672);
//    }
//}