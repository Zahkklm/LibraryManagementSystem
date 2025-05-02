package com.librarysystem.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Data Transfer Object (DTO) for login requests.
 * Used to transfer login credentials between auth-service and user-service.
 * Contains validation constraints to ensure data integrity.
 */
@Data  // Lombok annotation to generate getters, setters, equals, hashCode, and toString
public class LoginRequest {

    /**
     * User's email address used for authentication.
     * Must be a valid email format.
     * Cannot be null or blank.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    /**
     * User's password for authentication.
     * Cannot be null or blank.
     * Will be validated against stored encrypted password.
     */
    @NotBlank(message = "Password is required")
    private String password;
}