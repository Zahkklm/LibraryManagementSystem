package com.librarysystem.borrowservice.saga;

import com.librarysystem.borrowservice.repository.BorrowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Saga event listener for handling book reservation results.
 * 
 * Listens for book reservation events from the book-service and updates the borrow status accordingly.
 */
@Component
@RequiredArgsConstructor
public class BorrowSagaListener {
    private final BorrowRepository borrowRepository;

    /**
     * Handles successful book reservation events.
     * Sets the borrow status to "RESERVED" if the reservation succeeded.
     *
     * @param event the event data containing the borrowId
     */
    @KafkaListener(topics = "book-reserved", groupId = "borrow-saga")
    public void handleBookReserved(Map<String, Object> event) {
        String borrowId = (String) event.get("borrowId");
        borrowRepository.findById(borrowId).ifPresent(borrow -> {
            borrow.setStatus("RESERVED");
            borrowRepository.save(borrow);
        });
    }

    /**
     * Handles failed book reservation events.
     * Sets the borrow status to "FAILED" if the reservation failed.
     *
     * @param event the event data containing the borrowId
     */
    @KafkaListener(topics = "book-reserve-failed", groupId = "borrow-saga")
    public void handleBookReserveFailed(Map<String, Object> event) {
        String borrowId = (String) event.get("borrowId");
        borrowRepository.findById(borrowId).ifPresent(borrow -> {
            borrow.setStatus("FAILED");
            borrowRepository.save(borrow);
        });
    }
}