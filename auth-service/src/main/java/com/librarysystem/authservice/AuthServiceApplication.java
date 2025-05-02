package com.librarysystem.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Main application class for the Authentication Service.
 * This service is responsible for user authentication and JWT token management.
 *
 * Features:
 * - JWT-based authentication
 * - Integration with User Service via Feign
 * - Service discovery with Eureka
 * - Stateless authentication
 *
 * Port: 8084
 * Service Name: auth-service
 */
@SpringBootApplication  // Enables Spring Boot auto-configuration and component scanning
@EnableFeignClients    // Enables Feign clients for service-to-service communication
@EnableDiscoveryClient // Registers service with Eureka discovery server
public class AuthServiceApplication {

    /**
     * Main method that starts the Authentication Service.
     * Bootstraps Spring Boot application context.
     *
     * @param args Command line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}