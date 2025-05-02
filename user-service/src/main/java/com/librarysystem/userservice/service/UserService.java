package com.librarysystem.userservice.service;

import com.librarysystem.userservice.dto.UserCreateRequest;
import com.librarysystem.userservice.entity.User;
import com.librarysystem.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates a new user in the system.
     * Validates email uniqueness and encrypts the password before saving.
     *
     * @param request DTO containing user creation details
     * @return Created User entity
     * @throws RuntimeException if email already exists in the system
     */
    @Transactional
    public User createUser(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());

        return userRepository.save(user);
    }

    /**
     * Retrieves a user by their ID.
     *
     * @param id The unique identifier of the user
     * @return User entity if found
     * @throws RuntimeException if user is not found
     */
    public User getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found"));
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
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * Deactivates a user account.
     * Sets the active status to false but retains the user record.
     *
     * @param id The unique identifier of the user to deactivate
     * @throws RuntimeException if user is not found
     */
    @Transactional
    public void deactivateUser(Long id) {
        User user = getUserById(id);
        user.setActive(false);
        userRepository.save(user);
    }
}