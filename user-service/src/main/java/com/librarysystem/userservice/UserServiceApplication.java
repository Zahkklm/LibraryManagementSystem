package com.librarysystem.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Main application class for the User Service.
 * This service handles user management operations in the Library Management System.
 *
 * Features:
 * - User CRUD operations
 * - User authentication support
 * - Role-based access control
 * - Integration with Eureka service discovery
 *
 * Port: 8082
 * Service Name: user-service
 */
@SpringBootApplication
@EnableDiscoveryClient
public class UserServiceApplication {

    /**
     * Main method that starts the User Service application.
     * Bootstraps Spring Boot application context.
     *
     * @param args Command line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}