package com.librarysystem.userservice.service;

import com.librarysystem.userservice.dto.UserCreateRequest;
import com.librarysystem.userservice.entity.User;
import com.librarysystem.userservice.entity.UserRole;
import com.librarysystem.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private KeycloakAdminService keycloakAdminService;

    @InjectMocks
    private UserService userService;

    private UserCreateRequest userCreateRequest;
    private User user;
    private String keycloakId;

    @BeforeEach
    void setUp() {
        keycloakId = UUID.randomUUID().toString();

        userCreateRequest = new UserCreateRequest();
        userCreateRequest.setFirstName("John");
        userCreateRequest.setLastName("Doe");
        userCreateRequest.setEmail("john.doe@example.com");
        userCreateRequest.setPassword("password123");
        userCreateRequest.setRole(UserRole.MEMBER);

        user = new User();
        user.setId(keycloakId);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");
        user.setPassword("encodedPassword");
        user.setRole(UserRole.MEMBER);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("createUser - Success")
    void createUser_whenValidRequest_shouldCreateUserSuccessfully() throws Exception {
        when(userRepository.existsByEmail(userCreateRequest.getEmail())).thenReturn(false);
        when(keycloakAdminService.createKeycloakUser(any(UserCreateRequest.class), anyString())).thenReturn(keycloakId);
        when(passwordEncoder.encode(userCreateRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(keycloakId); // Ensure ID is set as it would be in real scenario
            return savedUser;
        });

        User createdUser = userService.createUser(userCreateRequest);

        assertNotNull(createdUser);
        assertEquals(keycloakId, createdUser.getId());
        assertEquals(userCreateRequest.getEmail(), createdUser.getEmail());
        assertEquals("encodedPassword", createdUser.getPassword());
        assertEquals(userCreateRequest.getRole(), createdUser.getRole());

        verify(userRepository).existsByEmail(userCreateRequest.getEmail());
        verify(keycloakAdminService).createKeycloakUser(userCreateRequest, userCreateRequest.getPassword());
        verify(passwordEncoder).encode(userCreateRequest.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("createUser - Email Already Exists")
    void createUser_whenEmailAlreadyExists_shouldThrowRuntimeException() {
        when(userRepository.existsByEmail(userCreateRequest.getEmail())).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.createUser(userCreateRequest);
        });

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository).existsByEmail(userCreateRequest.getEmail());
        verify(keycloakAdminService, never()).createKeycloakUser(any(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("createUser - Keycloak Creation Fails")
    void createUser_whenKeycloakCreationFails_shouldThrowRuntimeException() throws Exception {
        when(userRepository.existsByEmail(userCreateRequest.getEmail())).thenReturn(false);
        when(keycloakAdminService.createKeycloakUser(any(UserCreateRequest.class), anyString()))
            .thenThrow(new RuntimeException("Keycloak error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.createUser(userCreateRequest);
        });

        assertTrue(exception.getMessage().startsWith("User creation failed in Keycloak:"));
        verify(userRepository).existsByEmail(userCreateRequest.getEmail());
        verify(keycloakAdminService).createKeycloakUser(userCreateRequest, userCreateRequest.getPassword());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("getUserById - User Found")
    void getUserById_whenUserExists_shouldReturnUser() {
        when(userRepository.findById(keycloakId)).thenReturn(Optional.of(user));

        User foundUser = userService.getUserById(keycloakId);

        assertNotNull(foundUser);
        assertEquals(keycloakId, foundUser.getId());
        verify(userRepository).findById(keycloakId);
    }

    @Test
    @DisplayName("getUserById - User Not Found")
    void getUserById_whenUserDoesNotExist_shouldThrowRuntimeException() {
        when(userRepository.findById(keycloakId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.getUserById(keycloakId);
        });

        assertEquals("User not found with ID: " + keycloakId, exception.getMessage());
        verify(userRepository).findById(keycloakId);
    }

    @Test
    @DisplayName("getUserByEmail - User Found")
    void getUserByEmail_whenUserExists_shouldReturnUser() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        User foundUser = userService.getUserByEmail(user.getEmail());

        assertNotNull(foundUser);
        assertEquals(user.getEmail(), foundUser.getEmail());
        verify(userRepository).findByEmail(user.getEmail());
    }

    @Test
    @DisplayName("getUserByEmail - User Not Found")
    void getUserByEmail_whenUserDoesNotExist_shouldThrowRuntimeException() {
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.getUserByEmail(email);
        });

        assertEquals("User not found with email: " + email, exception.getMessage());
        verify(userRepository).findByEmail(email);
    }

    @Test
    @DisplayName("validatePassword - Passwords Match")
    void validatePassword_whenPasswordsMatch_shouldReturnTrue() {
        String rawPassword = "password123";
        String encodedPassword = "encodedPassword";
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);

        boolean isValid = userService.validatePassword(rawPassword, encodedPassword);

        assertTrue(isValid);
        verify(passwordEncoder).matches(rawPassword, encodedPassword);
    }

    @Test
    @DisplayName("validatePassword - Passwords Do Not Match")
    void validatePassword_whenPasswordsDoNotMatch_shouldReturnFalse() {
        String rawPassword = "wrongPassword";
        String encodedPassword = "encodedPassword";
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(false);

        boolean isValid = userService.validatePassword(rawPassword, encodedPassword);

        assertFalse(isValid);
        verify(passwordEncoder).matches(rawPassword, encodedPassword);
    }

    @Test
    @DisplayName("deactivateUser - Success")
    void deactivateUser_whenUserExists_shouldDeactivateUser() {
        when(userRepository.findById(keycloakId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user); // Mock save operation

        userService.deactivateUser(keycloakId);

        assertFalse(user.isActive()); // Check if the user object was modified
        verify(userRepository).findById(keycloakId);
        verify(userRepository).save(user);
        // TODO: Add verification for keycloakAdminService.deactivateKeycloakUser(user.getEmail()) when implemented
    }

    @Test
    @DisplayName("deactivateUser - User Not Found")
    void deactivateUser_whenUserDoesNotExist_shouldThrowRuntimeException() {
        when(userRepository.findById(keycloakId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.deactivateUser(keycloakId);
        });

        assertEquals("User not found with ID: " + keycloakId, exception.getMessage());
        verify(userRepository).findById(keycloakId);
        verify(userRepository, never()).save(any());
    }
}