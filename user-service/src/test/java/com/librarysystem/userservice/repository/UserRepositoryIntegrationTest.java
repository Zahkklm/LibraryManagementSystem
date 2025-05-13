package com.librarysystem.userservice.repository;

import com.librarysystem.userservice.entity.User;
import com.librarysystem.userservice.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager; // Import TestEntityManager

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager; // Use TestEntityManager for more control

    @Autowired
    private UserRepository userRepository;

    @Test
    void saveAndFindUserByEmail_whenUserIsValid_shouldPersistAndRetrieveUser() {
        // Arrange
        String userId = UUID.randomUUID().toString();
        String userEmail = "test.user@example.com";

        User newUser = new User();
        newUser.setId(userId);
        newUser.setFirstName("Test");
        newUser.setLastName("User");
        newUser.setEmail(userEmail);
        newUser.setPassword("encodedSecurePassword123");
        newUser.setRole(UserRole.MEMBER);
        // 'active' and 'createdAt' will be set by @PrePersist in the User entity

        // Act
        User savedUser = entityManager.persistAndFlush(newUser); // Persist and flush to ensure it's written

        Optional<User> foundOptional = userRepository.findByEmail(userEmail);

        // Assert
        assertTrue(foundOptional.isPresent(), "User should be found by email");
        User foundUser = foundOptional.get();

        assertEquals(savedUser.getId(), foundUser.getId(), "ID should match");
        assertEquals(userId, foundUser.getId(), "ID should match the initially set ID");
        assertEquals("Test", foundUser.getFirstName(), "First name should match");
        assertEquals("User", foundUser.getLastName(), "Last name should match");
        assertEquals(userEmail, foundUser.getEmail(), "Email should match");
        assertEquals("encodedSecurePassword123", foundUser.getPassword(), "Password should match");
        assertEquals(UserRole.MEMBER, foundUser.getRole(), "Role should match");
        assertTrue(foundUser.isActive(), "User should be active by default due to @PrePersist");
        assertNotNull(foundUser.getCreatedAt(), "CreatedAt should be set by @PrePersist");
        assertTrue(foundUser.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(1)) &&
                   foundUser.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(5)),
                   "CreatedAt should be a recent timestamp");
    }

    @Test
    void findByEmail_whenUserDoesNotExist_shouldReturnEmptyOptional() {
        // Act
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        // Assert
        assertFalse(found.isPresent(), "Optional should be empty for a non-existent email");
    }

    @Test
    void existsByEmail_whenUserExists_shouldReturnTrue() {
        // Arrange
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setFirstName("Existing");
        user.setLastName("User");
        user.setEmail("existing.user@example.com");
        user.setPassword("password");
        user.setRole(UserRole.LIBRARIAN);
        entityManager.persistAndFlush(user);

        // Act
        boolean exists = userRepository.existsByEmail("existing.user@example.com");

        // Assert
        assertTrue(exists, "existsByEmail should return true for an existing user");
    }

    @Test
    void existsByEmail_whenUserDoesNotExist_shouldReturnFalse() {
        // Act
        boolean exists = userRepository.existsByEmail("another.nonexistent@example.com");

        // Assert
        assertFalse(exists, "existsByEmail should return false for a non-existent user");
    }
}