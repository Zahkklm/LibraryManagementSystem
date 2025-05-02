package com.librarysystem.authservice.config;

import com.librarysystem.authservice.client.UserServiceClient;
import com.librarysystem.authservice.dto.LoginRequest;
import com.librarysystem.authservice.security.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration  // Changed to TestConfiguration
public class TestConfig {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @Bean
    @Primary  // Only one Primary bean for JwtUtils
    public JwtUtils jwtUtils() {
        return new JwtUtils(secret, expirationMs);
    }

    @Bean
    @Primary  // Only one Primary bean for UserServiceClient
    public UserServiceClient userServiceClient() {
        return request -> "test@example.com".equals(request.getEmail()) 
            && "password123".equals(request.getPassword());
    }
}