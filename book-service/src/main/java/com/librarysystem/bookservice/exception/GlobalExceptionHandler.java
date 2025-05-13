package com.librarysystem.bookservice.exception;

import com.librarysystem.bookservice.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for the Book Service.
 * <p>
 * This class provides centralized exception handling across all controllers
 * in the Book Service application. It transforms various exceptions into
 * standardized {@link ErrorResponse} objects with appropriate HTTP status codes,
 * making the API more consistent and user-friendly.
 * <p>
 * The exception handler follows these principles:
 * <ul>
 *   <li>Maps application-specific and common Spring exceptions to appropriate HTTP status codes.</li>
 *   <li>Provides clear, user-friendly error messages.</li>
 *   <li>Includes relevant error details (like validation failures) while hiding implementation specifics for security.</li>
 *   <li>Logs exceptions at appropriate severity levels for monitoring and debugging.</li>
 *   <li>Maintains consistency with the API's error response format defined by {@link ErrorResponse}.</li>
 * </ul>
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles validation exceptions triggered by {@code @Valid} on controller method arguments.
     * <p>
     * When DTO validation fails (e.g., a required field is missing or a format is incorrect),
     * Spring throws a {@link MethodArgumentNotValidException}. This handler catches it,
     * extracts detailed field-specific error messages, and returns a 400 Bad Request response.
     *
     * @param ex The {@link MethodArgumentNotValidException} containing binding and validation errors.
     * @param request The current {@link HttpServletRequest} that led to the exception.
     * @return A {@link ResponseEntity} containing an {@link ErrorResponse} with HTTP status 400 (Bad Request)
     *         and a list of validation error details.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        logger.warn("Validation error for request URI {}: {}", request.getRequestURI(), ex.getMessage());
        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "Input validation failed for one or more fields.",
                request.getRequestURI(),
                details
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles illegal argument exceptions, typically used for business rule violations or invalid input
     * that is not caught by standard DTO validation.
     * <p>
     * This handler is primarily used for cases like ISBN conflicts (where a book with
     * the same ISBN already exists) or other business logic validations that result in an
     * {@link IllegalArgumentException}. It returns a 409 Conflict status for such scenarios.
     *
     * @param ex The {@link IllegalArgumentException} that was thrown.
     * @param request The current {@link HttpServletRequest} that led to the exception.
     * @return A {@link ResponseEntity} containing an {@link ErrorResponse} with HTTP status 409 (Conflict).
     *         If the exception message suggests a different context, a 400 Bad Request might be considered.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        logger.warn("Illegal argument for request URI {}: {}", request.getRequestURI(), ex.getMessage());
        // Defaulting to CONFLICT for business rule violations like duplicate ISBN.
        // Could be refined to return BAD_REQUEST for other types of illegal arguments if needed.
        HttpStatus status = HttpStatus.CONFLICT;

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(), // The message from the service layer is usually informative here
                request.getRequestURI(),
                null
        );
        return new ResponseEntity<>(errorResponse, status);
    }

    /**
     * Handles {@link AccessDeniedException} thrown by Spring Security when a user attempts
     * an action for which they do not have sufficient privileges.
     * <p>
     * This typically occurs when {@code @PreAuthorize} or other security annotations deny access.
     * The handler returns a 403 Forbidden status.
     *
     * @param ex The {@link AccessDeniedException} that was thrown.
     * @param request The current {@link HttpServletRequest} that led to the exception.
     * @return A {@link ResponseEntity} containing an {@link ErrorResponse} with HTTP status 403 (Forbidden).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        logger.warn("Access denied for request URI {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                "Access Denied: You do not have the required permissions to perform this action.",
                request.getRequestURI(),
                null
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    /**
     * Handles general {@link RuntimeException} instances, with specific logic for "not found" scenarios.
     * <p>
     * This handler attempts to interpret the exception message. If the message indicates
     * that a resource was "not found", it returns a 404 Not Found status. Otherwise,
     * it defaults to a 500 Internal Server Error. This allows service methods to throw
     * simple {@code RuntimeException("Resource not found with id: X")} and have it mapped
     * to the correct HTTP status.
     *
     * @param ex The {@link RuntimeException} that was thrown.
     * @param request The current {@link HttpServletRequest} that led to the exception.
     * @return A {@link ResponseEntity} containing an {@link ErrorResponse} with an appropriate
     *         HTTP status (404 Not Found or 500 Internal Server Error).
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleGenericRuntimeException(RuntimeException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "An unexpected runtime error occurred.";

        if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("not found")) {
            status = HttpStatus.NOT_FOUND;
            message = ex.getMessage(); // Use the specific "not found" message
            logger.warn("Resource not found for request URI {}: {}", request.getRequestURI(), ex.getMessage());
        } else {
            logger.error("Runtime exception for request URI {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        }

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                null
        );
        return new ResponseEntity<>(errorResponse, status);
    }

    /**
     * Fallback handler for all other unhandled exceptions.
     * <p>
     * This is the last line of defense in the exception handling strategy.
     * It catches any exceptions not handled by more specific handlers (e.g., checked exceptions
     * or errors not subclassing {@link RuntimeException}). It logs them as critical errors
     * and returns a generic 500 Internal Server Error message to avoid
     * leaking sensitive implementation details or stack traces to the client.
     *
     * @param ex The unhandled {@link Exception}.
     * @param request The current {@link HttpServletRequest} that led to the exception.
     * @return A {@link ResponseEntity} containing an {@link ErrorResponse} with HTTP status 500 (Internal Server Error).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex, HttpServletRequest request) {
        logger.error("An critical unhandled exception occurred for request URI {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "An unexpected internal error occurred. Please try again later or contact support.",
                request.getRequestURI(),
                null
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Example placeholder for a custom ResourceNotFoundException if you define one.
    // /**
    //  * Handles custom {@code ResourceNotFoundException} for cases where a specific resource
    //  * could not be located.
    //  *
    //  * @param ex The {@code ResourceNotFoundException} that was thrown.
    //  * @param request The current {@link HttpServletRequest} that led to the exception.
    //  * @return A {@link ResponseEntity} containing an {@link ErrorResponse} with HTTP status 404 (Not Found).
    //  */
    // @ExceptionHandler(ResourceNotFoundException.class) // You would need to create this custom exception
    // public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
    //     logger.warn("Resource not found for request URI {}: {}", request.getRequestURI(), ex.getMessage());
    //     ErrorResponse errorResponse = new ErrorResponse(
    //             LocalDateTime.now(),
    //             HttpStatus.NOT_FOUND.value(),
    //             HttpStatus.NOT_FOUND.getReasonPhrase(),
    //             ex.getMessage(),
    //             request.getRequestURI(),
    //             null
    //     );
    //     return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    // }
}