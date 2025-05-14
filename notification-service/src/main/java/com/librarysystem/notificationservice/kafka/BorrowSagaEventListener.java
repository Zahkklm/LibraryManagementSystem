package com.librarysystem.notificationservice.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.librarysystem.notificationservice.event.NotificationEvent;
import com.librarysystem.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Listener for borrow-related SAGA events that generates notifications.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BorrowSagaEventListener {
    
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    
    /**
     * Listens for successful book reservations and notifies the user.
     */
    @KafkaListener(topics = "book-reserved", groupId = "notification-group")
    public void handleBookReserved(Map<String, Object> event) {
        try {
            // Ideally, these would include more user-friendly data
            String borrowId = (String) event.get("borrowId");
            Long bookId = ((Number) event.get("bookId")).longValue();
            String userId = (String) event.get("userId"); // The borrow service should include this
            String bookTitle = (String) event.get("bookTitle"); // This might need to be enriched
            
            if (userId == null) {
                log.warn("No user ID in book-reserved event, cannot send notification");
                return;
            }
            
            String bookName = bookTitle != null ? bookTitle : "Book #" + bookId;
            
            NotificationEvent notification = NotificationEvent.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .type("BOOK_RESERVED")
                .message("Your reservation for \"" + bookName + "\" was successful")
                .details(objectMapper.writeValueAsString(event))
                .timestamp(LocalDateTime.now())
                .bookId(bookId.toString())
                .borrowId(borrowId)
                .build();
                
            notificationService.saveAndPublishNotification(notification).subscribe();
            
        } catch (Exception e) {
            log.error("Failed to process book-reserved event", e);
        }
    }
    
    /**
     * Listens for failed book reservations and notifies the user.
     */
    @KafkaListener(topics = "book-reserve-failed", groupId = "notification-group")
    public void handleBookReserveFailed(Map<String, Object> event) {
        try {
            String borrowId = (String) event.get("borrowId");
            String userId = (String) event.get("userId"); // The borrow service should include this
            
            if (userId == null) {
                log.warn("No user ID in book-reserve-failed event, cannot send notification");
                return;
            }
            
            NotificationEvent notification = NotificationEvent.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .type("RESERVATION_FAILED")
                .message("Your book reservation could not be completed. The book may be unavailable.")
                .details(objectMapper.writeValueAsString(event))
                .timestamp(LocalDateTime.now())
                .borrowId(borrowId)
                .build();
                
            notificationService.saveAndPublishNotification(notification).subscribe();
            
        } catch (Exception e) {
            log.error("Failed to process book-reserve-failed event", e);
        }
    }
    
    /**
     * Listens for book returns and notifies the user.
     */
    @KafkaListener(topics = "book-returned", groupId = "notification-group")
    public void handleBookReturned(Map<String, Object> event) {
        try {
            // Extract data from event
            String userId = (String) event.get("userId");
            Long bookId = event.get("bookId") != null ? ((Number) event.get("bookId")).longValue() : null;
            String bookTitle = (String) event.get("bookTitle");
            
            if (userId == null || bookId == null) {
                log.warn("Missing user ID or book ID in book-returned event");
                return;
            }
            
            String bookName = bookTitle != null ? bookTitle : "Book #" + bookId;
            
            NotificationEvent notification = NotificationEvent.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .type("BOOK_RETURNED")
                .message("You have successfully returned \"" + bookName + "\"")
                .details(objectMapper.writeValueAsString(event))
                .timestamp(LocalDateTime.now())
                .bookId(bookId.toString())
                .build();
                
            notificationService.saveAndPublishNotification(notification).subscribe();
            
        } catch (Exception e) {
            log.error("Failed to process book-returned event", e);
        }
    }
}
