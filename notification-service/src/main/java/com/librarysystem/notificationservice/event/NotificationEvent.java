package com.librarysystem.notificationservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a notification event in the system.
 * <p>
 * This model is used for transmitting notification data via reactive streams
 * and Server-Sent Events (SSE). It contains all information necessary to display
 * and process a notification on the client side.
 * <p>
 * Each notification is associated with a specific user and can optionally
 * reference related entities such as books or borrows.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    /**
     * Unique identifier for the notification
     */
    private String id;
    
    /**
     * ID of the user this notification is intended for
     */
    private String userId;
    
    /**
     * Type of the notification (e.g., BOOK_BORROWED, BOOK_RETURNED, BOOK_OVERDUE)
     */
    private String type;
    
    /**
     * Human-readable notification message
     */
    private String message;
    
    /**
     * Additional JSON-structured details about the event
     */
    private String details;
    
    /**
     * Timestamp when the notification was created
     */
    private LocalDateTime timestamp;
    
    /**
     * Optional reference to the related book's ID
     */
    private String bookId;
    
    /**
     * Optional reference to the related borrow transaction's ID
     */
    private String borrowId;
}
