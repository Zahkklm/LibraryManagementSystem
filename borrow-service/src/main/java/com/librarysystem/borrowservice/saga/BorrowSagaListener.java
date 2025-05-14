package com.librarysystem.borrowservice.saga;

import com.librarysystem.borrowservice.entity.Borrow;
import com.librarysystem.borrowservice.repository.BorrowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Saga event listener for handling book reservation results.
 * 
 * Listens for book reservation events from the book-service and updates the borrow status accordingly.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BorrowSagaListener {
    private final BorrowRepository borrowRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Handles successful book reservation events.
     * Sets the borrow status to "RESERVED" if the reservation succeeded.
     *
     * @param event the event data containing the borrowId and additional book information
     */
    @KafkaListener(topics = "book-reserved", groupId = "borrow-saga")
    public void handleBookReserved(Map<String, Object> event) {
        String borrowId = (String) event.get("borrowId");
        String bookTitle = (String) event.get("bookTitle");
        String author = (String) event.get("author");
        String isbn = (String) event.get("isbn");
        
        log.info("Received book-reserved event for borrow ID: {} - Book: '{}'", borrowId, bookTitle);
        
        borrowRepository.findById(borrowId).ifPresent(borrow -> {
            if (!"RESERVED".equals(borrow.getStatus())) {
                borrow.setStatus("RESERVED");
                
                // Set due date (e.g., 14 days from now)
                LocalDate dueDate = LocalDate.now().plusDays(14);
                borrow.setDueDate(dueDate);
                
                Borrow savedBorrow = borrowRepository.save(borrow);
                
                // Publish enriched event for notification service
                Map<String, Object> borrowConfirmedEvent = new HashMap<>();
                borrowConfirmedEvent.put("borrowId", borrowId);
                borrowConfirmedEvent.put("bookId", borrow.getBookId());
                borrowConfirmedEvent.put("userId", borrow.getUserId());
                borrowConfirmedEvent.put("bookTitle", bookTitle);
                borrowConfirmedEvent.put("author", author);
                borrowConfirmedEvent.put("isbn", isbn);
                borrowConfirmedEvent.put("dueDate", dueDate.toString());
                
                kafkaTemplate.send("borrow-confirmed", borrowConfirmedEvent);
                log.info("Published borrow-confirmed event for user {} - Book: '{}'", borrow.getUserId(), bookTitle);
            }
        });
    }

    /**
     * Handles failed book reservation events.
     * Sets the borrow status to "FAILED" if the reservation failed.
     * 
     * @param event the event data containing the borrowId and failure reason
     */
    @KafkaListener(topics = "book-reserve-failed", groupId = "borrow-saga")
    public void handleBookReserveFailed(Map<String, Object> event) {
        String borrowId = (String) event.get("borrowId");
        String reason = (String) event.get("reason");
        String bookTitle = (String) event.get("bookTitle");
        
        log.info("Received book-reserve-failed event for borrow ID: {}", borrowId);
        
        borrowRepository.findById(borrowId).ifPresent(borrow -> {
            if (!"RESERVED".equals(borrow.getStatus()) && !"FAILED".equals(borrow.getStatus())) {
                borrow.setStatus("FAILED");
                borrowRepository.save(borrow);
                
                // Publish enriched event for notification service
                Map<String, Object> borrowFailedEvent = new HashMap<>();
                borrowFailedEvent.put("borrowId", borrowId);
                borrowFailedEvent.put("bookId", borrow.getBookId());
                borrowFailedEvent.put("userId", borrow.getUserId());
                borrowFailedEvent.put("bookTitle", bookTitle != null ? bookTitle : "Unknown book");
                borrowFailedEvent.put("reason", reason != null ? reason : "Unknown reason");
                
                kafkaTemplate.send("borrow-failed", borrowFailedEvent);
                log.info("Published borrow-failed event for user {}", borrow.getUserId());
            }           
        });
    }
}