package com.librarysystem.userservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity class representing a user in the library management system.
 * This class maps to the 'users' table in the 'users' schema of the database.
 * Contains all necessary user information and status fields.
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "users", schema = "users")
public class User {
    
    /**
     * Unique identifier for the user.
     * Auto-generated using database sequence.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User's first name.
     * Cannot be null or empty string.
     */
    @NotBlank
    @Column(nullable = false)
    private String firstName;

    /**
     * User's last name.
     * Cannot be null or empty string.
     */
    @NotBlank
    @Column(nullable = false)
    private String lastName;

    /**
     * User's email address.
     * Must be unique in the system.
     * Used as the username for authentication.
     * Must be a valid email format.
     */
    @NotBlank
    @Email
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * User's encrypted password.
     * Stored using BCrypt encryption.
     * Cannot be null or empty.
     */
    @NotBlank
    @Column(nullable = false)
    private String password;

    /**
     * User's role in the system.
     * Stored as a string representation of UserRole enum.
     * Cannot be null.
     * @see UserRole
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    /**
     * Timestamp when the user account was created.
     * Automatically set during entity creation.
     * @see #onCreate()
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * Flag indicating if the user account is active.
     * True by default when user is created.
     * False indicates a deactivated account.
     */
    @Column(nullable = false)
    private boolean active;

    /**
     * Lifecycle callback method executed before persisting the entity.
     * Sets default values for createdAt and active fields.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        active = true;
    }
}