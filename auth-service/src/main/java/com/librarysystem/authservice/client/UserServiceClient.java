package com.librarysystem.authservice.client;

import com.librarysystem.authservice.dto.LoginRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client interface for communicating with the user-service.
 * Provides declarative REST client for user-service endpoints.
 * Uses circuit breaker pattern for fault tolerance.
 */
@FeignClient(
    name = "user-service",           // Service name registered in Eureka
    fallback = UserServiceClientFallback.class  // Fallback implementation for circuit breaker
)
public interface UserServiceClient {
    
    /**
     * Validates user credentials against user-service database.
     * 
     * @param request DTO containing email and password to validate
     * @return true if credentials are valid, false otherwise
     */
    @PostMapping("/api/users/validate")
    boolean validateCredentials(@RequestBody LoginRequest request);
}