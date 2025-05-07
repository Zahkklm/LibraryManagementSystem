package com.librarysystem.authservice.client;

import com.librarysystem.authservice.config.FeignClientConfig;
import com.librarysystem.authservice.dto.LoginRequest;
import com.librarysystem.authservice.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client interface for communicating with the user-service.
 * Provides declarative REST client for user-service endpoints.
 * Uses circuit breaker pattern for fault tolerance.
 */
@FeignClient(
    name = "user-service",           // Service name registered in Eureka
    path = "/api/users",            // Base path for all user-service endpoints
    configuration = FeignClientConfig.class,
    fallback = UserServiceClientFallback.class
)
public interface UserServiceClient {
    
    /**
     * Validates user credentials against user-service database.
     * 
     * @param request DTO containing email and password to validate
     * @return true if credentials are valid, false otherwise
     */
    @PostMapping("/validate")
    boolean validateCredentials(@RequestBody LoginRequest request);

    /**
     * Retrieves user details by email address.
     * 
     * @param email The email address to look up
     * @return UserDTO containing user details if found
     */
    @GetMapping("/email/{email}")
    UserDTO getUserByEmail(@PathVariable String email);
}