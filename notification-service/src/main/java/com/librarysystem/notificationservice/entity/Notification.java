package com.librarysystem.notificationservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Database entity representing a notification stored in the system.
 * <p>
 * This entity maps to the "notifications" table in the database and is used for
 * persistent storage of notifications sent to users. It uses Spring Data R2DBC
 * for reactive database operations.
 * <p>
 * Each notification is associated with a specific user and can optionally
 * reference related entities such as books or borrows.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("notifications")
public class Notification {
    /**
     * Unique identifier for the notification record
     */
    @Id
    private String id;
    
    /**
     * ID of the user this notification is intended for
     */
    private String userId;
    
    /**
     * Type of the notification (e.g., BOOK_RESERVED, BOOK_RETURNED, BOOK_OVERDUE)
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
     * Flag indicating whether the notification has been read by the user
     */
    private boolean read;
    
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