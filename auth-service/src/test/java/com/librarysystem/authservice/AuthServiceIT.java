package com.librarysystem.authservice;

import com.librarysystem.authservice.dto.LoginRequest;
import com.librarysystem.authservice.dto.JwtResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Auth Service.
 * Uses TestContainers for PostgreSQL database.
 * Tests end-to-end authentication flow.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
public class AuthServiceIT {

    /**
     * PostgreSQL container for integration tests.
     * Configured with test database credentials.
     */
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("auth_test")
        .withUsername("test")
        .withPassword("test");

    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * Configures dynamic properties for test database connection.
     * Called before tests start to set up database properties.
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    /**
     * Tests successful login flow.
     * Verifies that valid credentials return JWT token.
     */
    @Test
    void whenValidCredentials_thenReturnsToken() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        // Act
        var response = restTemplate.postForEntity(
            "/api/auth/login",
            request,
            JwtResponse.class
        );

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().getToken()).isNotNull();
    }

    /**
     * Tests failed login attempt.
     * Verifies that invalid credentials return 401 status.
     */
    @Test
    void whenInvalidCredentials_thenReturns401() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("invalid@example.com");
        request.setPassword("wrongpassword");

        // Act
        var response = restTemplate.postForEntity(
            "/api/auth/login",
            request,
            JwtResponse.class
        );

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }
}