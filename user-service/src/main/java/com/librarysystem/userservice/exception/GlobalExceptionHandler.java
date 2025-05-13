package com.librarysystem.userservice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String VALIDATION_FAILED_MESSAGE = "Input validation failed for one or more fields.";

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", HttpStatus.valueOf(status.value()).getReasonPhrase());
        body.put("message", VALIDATION_FAILED_MESSAGE);

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(x -> x.getField() + ": " + x.getDefaultMessage())
                .collect(Collectors.toList());
        body.put("details", errors);
        
        log.warn("Validation failed for request {}: {}", request.getDescription(false), errors);
        return new ResponseEntity<>(body, headers, status);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", HttpStatus.FORBIDDEN.getReasonPhrase());
        body.put("message", ex.getMessage()); // Typically "Access is denied"
        body.put("path", request.getDescription(false).substring(4)); 
        
        log.warn("Access Denied: {} for path {}", ex.getMessage(), body.get("path"));
        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(RuntimeException.class) 
    public ResponseEntity<Object> handleGenericRuntimeException(RuntimeException ex, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR; 
        String message = ex.getMessage();
        String path = request.getDescription(false).substring(4);

        if (message != null) {
            if (message.toLowerCase().contains("not found")) {
                status = HttpStatus.NOT_FOUND;
                log.warn("Resource not found for path {}: {}", path, message);
            } else if (message.toLowerCase().contains("already exists") || message.toLowerCase().contains("conflict")) {
                status = HttpStatus.CONFLICT;
                log.warn("Conflict detected for path {}: {}", path, message);
            } else {
                // For other RuntimeExceptions that are not specifically handled above and result in 500
                log.error("Unhandled RuntimeException for path {}: {}", path, message, ex);
            }
        } else {
             log.error("Unhandled RuntimeException with no message for path {}: {}", path, ex.getClass().getName(), ex);
             message = "An unexpected error occurred"; // Provide a generic message if null
        }
        
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message); 
        body.put("path", path);

        return new ResponseEntity<>(body, status);
    }

    // TODO: Add more specific custom exception handlers here if you define them
    // e.g., @ExceptionHandler(UserNotFoundException.class), @ExceptionHandler(EmailAlreadyExistsException.class)
    // These would provide more targeted logging and potentially different error structures if needed.
}