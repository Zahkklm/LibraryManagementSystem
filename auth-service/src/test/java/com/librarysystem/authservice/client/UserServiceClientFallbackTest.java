package com.librarysystem.authservice.client;

import com.librarysystem.authservice.dto.LoginRequest;
import com.librarysystem.authservice.dto.UserCreateRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceClientFallbackTest {

    private final UserServiceClientFallback fallback = new UserServiceClientFallback();

    @Test
    void validateCredentials_returnsFalse() {
        assertFalse(fallback.validateCredentials(new LoginRequest("email", "pass")));
    }

    @Test
    void getUserByEmail_returnsNull() {
        assertNull(fallback.getUserByEmail("test@example.com"));
    }

    @Test
    void createUser_returnsNull() {
        assertNull(fallback.createUser(new UserCreateRequest()));
    }
}