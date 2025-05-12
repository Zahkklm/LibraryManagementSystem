package com.librarysystem.borrowservice.controller;

import com.librarysystem.borrowservice.entity.Borrow;
import com.librarysystem.borrowservice.repository.BorrowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.time.LocalDate;
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
        Borrow borrow = new Borrow(borrowId, userId, bookId, "PENDING", LocalDate.now());
        borrowRepository.save(borrow);

        // Publish event to Kafka
        kafkaTemplate.send("book-reserve-requested", Map.of(
            "borrowId", borrowId,
            "bookId", bookId,
            "userId", userId
        ));
        return ResponseEntity.ok("Borrow request submitted: " + borrowId);
    }

    /**
     * Endpoint to cancel a borrow request (compensating action).
     *
     * @param borrowId the ID of the borrow to cancel
     * @return a response indicating the result
     */
    @PostMapping("/{borrowId}/cancel")
    public ResponseEntity<String> cancelBorrow(@PathVariable String borrowId) {
        return borrowRepository.findById(borrowId).map(borrow -> {
            if ("RESERVED".equals(borrow.getStatus())) {
                borrow.setStatus("CANCELLED");
                borrowRepository.save(borrow);
                // Publish compensation event to Kafka
                kafkaTemplate.send("book-reservation-cancelled", Map.of(
                    "borrowId", borrowId,
                    "bookId", borrow.getBookId()
                ));
                return ResponseEntity.ok("Borrow cancelled and compensation event sent.");
            } else {
                return ResponseEntity.badRequest().body("Borrow cannot be cancelled in its current state.");
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Endpoint to return a borrowed book.
     *
     * @param borrowId the ID of the borrow to return
     * @return a response indicating the result
     */
    @PostMapping("/{borrowId}/return")
    public ResponseEntity<String> returnBook(@PathVariable String borrowId) {
        return borrowRepository.findById(borrowId).map(borrow -> {
            if ("RESERVED".equals(borrow.getStatus())) {
                borrow.setStatus("RETURNED");
                borrow.setReturnDate(LocalDate.now());
                borrowRepository.save(borrow);
                // Publish return event to Kafka
                kafkaTemplate.send("book-returned", Map.of(
                    "borrowId", borrowId,
                    "bookId", borrow.getBookId()
                ));
                return ResponseEntity.ok("Book returned and event sent.");
            } else {
                return ResponseEntity.badRequest().body("Book cannot be returned in its current state.");
            }
        }).orElse(ResponseEntity.notFound().build());
    }
}