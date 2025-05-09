package com.librarysystem.bookservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object (DTO) for standardized error responses in the Book Service API.
 * <p>
 * This class represents the structure of error responses returned to API consumers
 * when requests fail due to client errors (4xx) or server errors (5xx). It follows
 * REST API best practices by providing consistent, detailed error information including:
 * <ul>
 *   <li>When the error occurred (timestamp)</li>
 *   <li>HTTP status code (status)</li>
 *   <li>Error type (error)</li>
 *   <li>Human-readable error message (message)</li>
 *   <li>Request path that triggered the error (path)</li>
 *   <li>Detailed validation errors when applicable (details)</li>
 * </ul>
 * <p>
 * This standardized format enables API consumers to easily parse and handle errors
 * in a consistent manner across all Book Service endpoints.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {
    
    /**
     * The date and time when the error occurred.
     * Provides temporal context for debugging and logging.
     */
    private LocalDateTime timestamp;
    
    /**
     * The HTTP status code of the error.
     * Maps to standard HTTP status codes (e.g., 400, 404, 500).
     */
    private int status;
    
    /**
     * The type or category of error.
     * Examples include "Bad Request", "Not Found", or "Internal Server Error".
     */
    private String error;
    
    /**
     * A human-readable description of what went wrong.
     * Should be clear enough for API consumers to understand the issue.
     */
    private String message;
    
    /**
     * The API endpoint path that was accessed when the error occurred.
     * Useful for identifying which operation triggered the error.
     */
    private String path;
    
    /**
     * A list of specific error details, typically used for validation errors.
     * For validation failures, this contains individual constraint violations.
     * May be empty for non-validation errors.
     */
    private List<String> details;
}