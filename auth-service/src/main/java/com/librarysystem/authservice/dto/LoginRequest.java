package com.librarysystem.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Data Transfer Object for login requests.
 * Contains validated credentials for authentication.
 * Used by both auth-service and user-service.
 */
@Data  // Lombok annotation for getters, setters, equals, hashCode, toString
public class LoginRequest {
    
    /**
     * User's email address.
     * Must be a valid email format.
     * Cannot be null or empty.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    /**
     * User's password.
     * Cannot be null or empty.
     * Will be validated against stored hash.
     */
    @NotBlank(message = "Password is required")
    private String password;
}