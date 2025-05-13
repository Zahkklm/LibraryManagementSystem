package com.librarysystem.userservice.controller;

import com.librarysystem.userservice.dto.LoginRequest;
import com.librarysystem.userservice.dto.UserCreateRequest;
import com.librarysystem.userservice.dto.UserDTO;
import com.librarysystem.userservice.entity.User;
import com.librarysystem.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    /**
     * Endpoint to create a new user.
     * Assumes that the API Gateway has already validated the request.
     *
     * @param request The user creation request payload.
     * @return The created user as a DTO.
     */
    @PostMapping
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody UserCreateRequest request) {
        logger.info("Creating user with email: {}", request.getEmail());
        User user = userService.createUser(request);
        logger.info("User created successfully: {}", user.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserDTO.fromEntity(user));
    }

    /**
     * Endpoint to deactivate a user by ID.
     * Assumes that the API Gateway has already validated the request.
     *
     * @param id The ID of the user to deactivate.
     * @return A response indicating success.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateUser(@PathVariable String id) {
        logger.info("Deactivating user with ID: {}", id);
        userService.deactivateUser(id);
        logger.info("User with ID: {} deactivated successfully.", id);
        return ResponseEntity.ok().build();
    }

    /**
     * Endpoint to retrieve a user by ID.
     * Assumes that the API Gateway has already validated the request.
     *
     * @param id The ID of the user to retrieve.
     * @param currentUserId The ID of the user making the request (from X-User-Id header).
     * @param roles The roles of the user making the request (from X-User-Roles header).
     * @return The user as a DTO.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String currentUserId,
            @RequestHeader("X-User-Roles") String roles) {
        logger.info("Fetching user with ID: {}, requested by user: {}", id, currentUserId);

        boolean isAdmin = roles.contains("ADMIN");
        boolean isSelf = currentUserId.equals(id);

        if (!isAdmin && !isSelf) {
            logger.warn("Access denied: User {} attempted to access user {} data", currentUserId, id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        User user = userService.getUserById(id);
        logger.info("User with ID: {} found.", id);
        return ResponseEntity.ok(UserDTO.fromEntity(user));
    }

    /**
     * Endpoint to retrieve a user by email.
     * Assumes that the API Gateway has already validated the request.
     *
     * @param email The email of the user to retrieve.
     * @return The user as a DTO.
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<UserDTO> getUserByEmail(@PathVariable String email) {
        logger.info("Fetching user with email: {}", email);
        User user = userService.getUserByEmail(email);
        logger.info("User with email: {} found.", email);
        return ResponseEntity.ok(UserDTO.fromEntity(user));
    }

    /**
     * Endpoint to validate user credentials.
     * This is used by the auth-service for authentication purposes.
     *
     * @param request The login request containing email and password.
     * @return True if the credentials are valid, false otherwise.
     */
    @PostMapping("/validate")
    public ResponseEntity<Boolean> validateCredentials(@Valid @RequestBody LoginRequest request) {
        logger.info("Validating credentials for email: {}", request.getEmail());
        try {
            User user = userService.getUserByEmail(request.getEmail());
            boolean isValid = userService.validatePassword(request.getPassword(), user.getPassword()) && user.isActive();
            logger.info("Credentials validation result for {}: {}", request.getEmail(), isValid);
            return ResponseEntity.ok(isValid);
        } catch (RuntimeException e) {
            logger.warn("Validation failed for email {}: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.ok(false);
        }
    }
}