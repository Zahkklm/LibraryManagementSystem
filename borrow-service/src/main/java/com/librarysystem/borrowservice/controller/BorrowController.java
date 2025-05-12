package com.librarysystem.borrowservice.controller;

import com.librarysystem.borrowservice.entity.Borrow;
import com.librarysystem.borrowservice.repository.BorrowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.Map;

/**
 * REST controller for handling borrow requests.
 */
@RestController
@RequestMapping("/api/borrows")
@RequiredArgsConstructor
public class BorrowController {
    private final BorrowRepository borrowRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Endpoint to create a new borrow request.
     *
     * @param userId the ID of the user requesting the borrow
     * @param bookId the ID of the book to borrow
     * @return a response containing the borrow request ID
     */
    @PostMapping
    public ResponseEntity<String> borrowBook(@RequestParam Long userId, @RequestParam Long bookId) {
        String borrowId = UUID.randomUUID().toString();
        Borrow borrow = new Borrow(borrowId, userId, bookId, "PENDING");
        borrowRepository.save(borrow);

        // Publish event to Kafka
        kafkaTemplate.send("book-reserve-requested", Map.of(
            "borrowId", borrowId,
            "bookId", bookId,
            "userId", userId
        ));
        return ResponseEntity.ok("Borrow request submitted: " + borrowId);
    }
}