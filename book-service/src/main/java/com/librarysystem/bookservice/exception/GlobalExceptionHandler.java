package com.librarysystem.bookservice.exception;

import com.librarysystem.bookservice.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
 *   <li>Map application-specific exceptions to appropriate HTTP status codes</li>
 *   <li>Provide clear, user-friendly error messages</li>
 *   <li>Include relevant error details while hiding implementation specifics</li>
 *   <li>Log exceptions at appropriate severity levels</li>
 *   <li>Maintain consistency with the API's error response format</li>
 * </ul>
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Handle custom ResourceNotFoundException (you'd need to create this)
    // @ExceptionHandler(ResourceNotFoundException.class)
    // public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
    //     logger.warn("Resource not found: {}", ex.getMessage());
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

    /**
     * Handles illegal argument exceptions, typically used for business rule violations.
     * <p>
     * This handler is primarily used for cases like ISBN conflicts, where a book with
     * the same ISBN already exists, or other business rule violations that prevent an
     * operation from completing successfully.
     *
     * @param ex The IllegalArgumentException that was thrown
     * @param request The HTTP request being processed
     * @return A ResponseEntity containing an ErrorResponse with CONFLICT status
     */
    @ExceptionHandler(IllegalArgumentException.class) // Example for ISBN conflict
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        logger.warn("Illegal argument: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(), // Or BAD_REQUEST depending on context
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }
    
    /**
     * Handles general runtime exceptions with intelligent status code mapping.
     * <p>
     * This handler attempts to interpret the exception message to determine the
     * most appropriate HTTP status code. For example, if the message contains
     * "not found", it will return a 404 NOT_FOUND status rather than a generic
     * 500 error.
     * <p>
     * This approach allows service methods to throw simple RuntimeExceptions
     * while still providing appropriate HTTP semantics.
     *
     * @param ex The RuntimeException that was thrown
     * @param request The HTTP request being processed
     * @return A ResponseEntity with an appropriate status code based on the exception message
     */
    @ExceptionHandler(RuntimeException.class) // Catch-all for other runtime exceptions (like "Book not found")
    public ResponseEntity<ErrorResponse> handleGenericRuntimeException(RuntimeException ex, HttpServletRequest request) {
        logger.error("Runtime exception occurred: {}", ex.getMessage(), ex);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("not found")) {
            status = HttpStatus.NOT_FOUND;
        }

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
        return new ResponseEntity<>(errorResponse, status);
    }

    /**
     * Handles validation exceptions from request body and parameter validation.
     * <p>
     * When validation annotations like @Valid are used in controller methods,
     * this handler processes any validation failures. It extracts field-specific
     * error messages and returns them in a structured format, helping API clients
     * quickly identify and fix input problems.
     *
     * @param ex The validation exception containing binding results
     * @param request The HTTP request being processed
     * @return A ResponseEntity with BAD_REQUEST status and detailed validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        logger.warn("Validation error: {}", ex.getMessage());
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
     * Fallback handler for all unhandled exceptions.
     * <p>
     * This is the last line of defense in the exception handling strategy.
     * It catches any exceptions not handled by more specific handlers,
     * logs them as errors, and returns a generic 500 error message to avoid
     * leaking implementation details or stack traces to the client.
     *
     * @param ex The unhandled exception
     * @param request The HTTP request being processed
     * @return A ResponseEntity with INTERNAL_SERVER_ERROR status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex, HttpServletRequest request) {
        logger.error("An unexpected error occurred: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI(),
                null
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}