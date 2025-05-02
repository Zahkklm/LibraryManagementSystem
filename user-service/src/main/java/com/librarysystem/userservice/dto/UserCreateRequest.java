package com.librarysystem.userservice.dto;

import com.librarysystem.userservice.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Data Transfer Object (DTO) for creating new users.
 * This class represents the request payload when creating a new user in the system.
 * It includes validation constraints to ensure data integrity.
 */
@Data
public class UserCreateRequest {
    /**
     * The user's first name.
     * Must not be blank or null.
     */
    @NotBlank(message = "First name is required")
    private String firstName;

    /**
     * The user's last name.
     * Must not be blank or null.
     */
    @NotBlank(message = "Last name is required")
    private String lastName;

    /**
     * The user's email address.
     * Must be a valid email format and not blank or null.
     * This will be used as the unique identifier for the user.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    /**
     * The user's password.
     * Must not be blank or null.
     * Will be encrypted before storage.
     */
    @NotBlank(message = "Password is required")
    private String password;

    /**
     * The user's role in the system.
     * Defaults to MEMBER if not specified.
     * Possible values: ADMIN, LIBRARIAN, MEMBER
     */
    private UserRole role = UserRole.MEMBER;
}