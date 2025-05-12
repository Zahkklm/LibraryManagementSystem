package com.librarysystem.userservice.service;

import com.librarysystem.userservice.dto.UserCreateRequest;
import com.librarysystem.userservice.entity.User;
import com.librarysystem.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class handling user-related business logic.
 * Provides methods for user management operations.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KeycloakAdminService keycloakAdminService; // Inject KeycloakAdminService

    /**
     * Creates a new user in the system and synchronizes with Keycloak.
     * Validates email uniqueness and encrypts the password before saving locally.
     *
     * @param request DTO containing user creation details
     * @return Created User entity
     * @throws RuntimeException if email already exists in the system or Keycloak sync fails
     */
    @Transactional
    public User createUser(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            logger.warn("Attempt to create user with existing email: {}", request.getEmail());
            throw new RuntimeException("Email already exists");
        }

        // 1. Create user in Keycloak and get the UUID
        String keycloakUserId;
        try {
            keycloakUserId = keycloakAdminService.createKeycloakUser(request, request.getPassword());
            logger.info("User {} successfully created in Keycloak with ID {}", request.getEmail(), keycloakUserId);
        } catch (Exception e) {
            logger.error("Failed to create user {} in Keycloak. Local user will NOT be created.", request.getEmail(), e);
            throw new RuntimeException("User creation failed in Keycloak: " + e.getMessage(), e);
        }

        // 2. Create and save the local user with the Keycloak UUID
        User user = new User();
        user.setId(keycloakUserId);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        // 'active' and 'createdAt' are set by @PrePersist in User entity

        logger.info("Saving user {} to local database.", request.getEmail());
        User savedUserInDb = userRepository.save(user);
        logger.info("User {} saved locally with ID: {}", savedUserInDb.getEmail(), savedUserInDb.getId());

        return savedUserInDb;
    }

    /**
     * Retrieves a user by their ID.
     *
     * @param id The unique identifier of the user
     * @return User entity if found
     * @throws RuntimeException if user is not found
     */
    public User getUserById(String id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
    }

    /**
     * Retrieves a user by their email address.
     *
     * @param email The email address to search for
     * @return User entity if found
     * @throws RuntimeException if user is not found
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    /**
     * Validates a raw password against the hashed password stored in the database.
     *
     * @param rawPassword The raw password provided by the user.
     * @param encodedPassword The hashed password stored in the database.
     * @return True if the passwords match, false otherwise.
     */
    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * Deactivates a user account.
     * Sets the active status to false but retains the user record.
     * TODO: Also deactivate user in Keycloak if needed.
     *
     * @param id The unique identifier of the user to deactivate
     * @throws RuntimeException if user is not found
     */
    @Transactional
    public void deactivateUser(String id) { // Changed Long to String
        User user = getUserById(id);
        user.setActive(false);
        userRepository.save(user);
        logger.info("User with ID {} deactivated locally.", id);
        // TODO: Add logic here to deactivate the user in Keycloak as well
        // keycloakAdminService.deactivateKeycloakUser(user.getEmail());
    }
}