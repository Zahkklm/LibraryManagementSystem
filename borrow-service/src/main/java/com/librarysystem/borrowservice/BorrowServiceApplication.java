package com.librarysystem.borrowservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Borrow Service.
 * 
 * Starts the Spring Boot application for handling borrow requests and saga events.
 */
@SpringBootApplication
public class BorrowServiceApplication {

    /**
     * Application entry point.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(BorrowServiceApplication.class, args);
    }

}