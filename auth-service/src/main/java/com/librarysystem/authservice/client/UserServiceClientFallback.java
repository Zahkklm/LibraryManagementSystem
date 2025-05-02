package com.librarysystem.authservice.client;

import com.librarysystem.authservice.dto.LoginRequest;
import org.springframework.stereotype.Component;

/**
 * Fallback implementation for UserServiceClient.
 * Provides default behavior when user-service is unavailable.
 * Part of circuit breaker pattern implementation.
 */
@Component
public class UserServiceClientFallback implements UserServiceClient {
    
    /**
     * Default fallback behavior for credential validation.
     * Always returns false to fail securely when service is down.
     * 
     * @param request Login credentials (unused in fallback)
     * @return false to deny authentication during service outage
     */
    @Override
    public boolean validateCredentials(LoginRequest request) {
        return false;  // Fail securely when user-service is unavailable
    }
}