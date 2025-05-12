package com.librarysystem.userservice.repository;

import com.librarysystem.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for User entity operations.
 * Extends JpaRepository to inherit standard CRUD operations.
 * Provides custom query methods for user-specific operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    /**
     * Finds a user by their email address.
     * 
     * @param email The email address to search for
     * @return Optional<User> containing the user if found, empty otherwise
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Checks if a user exists with the given email address.
     * Used for validation during user registration.
     * 
     * @param email The email address to check
     * @return true if a user exists with the email, false otherwise
     */
    boolean existsByEmail(String email);
}