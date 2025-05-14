package com.librarysystem.borrowservice.controller;

import com.librarysystem.borrowservice.entity.Borrow;
import com.librarysystem.borrowservice.service.BorrowService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for handling borrow requests.
 * User ID and roles are extracted from the Spring Security context (populated by RoleExtractionFilter).
 * Now supports UUID user IDs from Keycloak.
 */
@Tag(name = "Borrow Management", description = "Endpoints for borrowing operations")
@RestController
@RequestMapping("/api/borrows")
@RequiredArgsConstructor
@Slf4j
public class BorrowController {

    private final BorrowService borrowService;

    /**
     * Endpoint to request borrowing a book.
     * @param bookId ID of the book to borrow
     * @param userId ID of the user borrowing the book
     * @return ResponseEntity with status and message
     */
    @PostMapping
    public ResponseEntity<String> borrowBook(
            @RequestParam Long bookId,
            @RequestParam String userId) {
        return borrowService.borrowBook(bookId, userId);
    }

    /**
     * Endpoint to cancel a borrow request.
     * @param borrowId ID of the borrow record to cancel
     * @return ResponseEntity with status and message
     */
    @PostMapping("/{borrowId}/cancel")
    public ResponseEntity<String> cancelBorrow(@PathVariable String borrowId) {
        return borrowService.cancelBorrow(borrowId);
    }

    /**
     * Endpoint to return a borrowed book.
     * @param borrowId ID of the borrow record to return
     * @return ResponseEntity with status and message
     */
    @PostMapping("/{borrowId}/return")
    public ResponseEntity<String> returnBook(@PathVariable String borrowId) {
        return borrowService.returnBook(borrowId);
    }

    /**
     * Endpoint to get details of a specific borrow record.
     * @param borrowId ID of the borrow record
     * @return ResponseEntity with the Borrow entity
     */
    @GetMapping("/{borrowId}")
    public ResponseEntity<Borrow> getBorrow(@PathVariable String borrowId) {
        return borrowService.getBorrow(borrowId);
    }

    /**
     * Endpoint to get borrowing history for a user.
     * @param userIdParam (optional) ID of the user; if not provided, uses current user
     * @return ResponseEntity with a list of Borrow entities
     */
    @GetMapping("/history")
    public ResponseEntity<List<Borrow>> getUserHistory(
            @RequestParam(value = "userId", required = false) String userIdParam) {
        return borrowService.getUserHistory(userIdParam);
    }

    /**
     * Endpoint to get borrowing history for all users (admin/librarian only).
     * @return ResponseEntity with a list of all Borrow entities
     */
    @GetMapping("/history/all")
    public ResponseEntity<List<Borrow>> getAllHistory() {
        return borrowService.getAllHistory();
    }

    /**
     * Endpoint to get all overdue borrows (admin/librarian only).
     * @return ResponseEntity with a list of overdue Borrow entities
     */
    @GetMapping("/overdue")
    public ResponseEntity<List<Borrow>> getOverdueBorrows() {
        return borrowService.getOverdueBorrows();
    }
}
