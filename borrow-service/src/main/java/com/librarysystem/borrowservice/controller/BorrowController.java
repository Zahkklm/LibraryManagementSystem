package com.librarysystem.borrowservice.controller;

import com.librarysystem.borrowservice.entity.Borrow;
import com.librarysystem.borrowservice.service.BorrowService;
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
@RestController
@RequestMapping("/api/borrows")
@RequiredArgsConstructor
@Slf4j
public class BorrowController {

    private final BorrowService borrowService;

    @PostMapping
    public ResponseEntity<String> borrowBook(
            @RequestParam Long bookId,
            @RequestParam String userId) {
        return borrowService.borrowBook(bookId, userId);
    }

    @PostMapping("/{borrowId}/cancel")
    public ResponseEntity<String> cancelBorrow(@PathVariable String borrowId) {
        return borrowService.cancelBorrow(borrowId);
    }

    @PostMapping("/{borrowId}/return")
    public ResponseEntity<String> returnBook(@PathVariable String borrowId) {
        return borrowService.returnBook(borrowId);
    }

    @GetMapping("/{borrowId}")
    public ResponseEntity<Borrow> getBorrow(@PathVariable String borrowId) {
        return borrowService.getBorrow(borrowId);
    }

    @GetMapping("/history")
    public ResponseEntity<List<Borrow>> getUserHistory(
            @RequestParam(value = "userId", required = false) String userIdParam) {
        return borrowService.getUserHistory(userIdParam);
    }

    @GetMapping("/history/all")
    public ResponseEntity<List<Borrow>> getAllHistory() {
        return borrowService.getAllHistory();
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<Borrow>> getOverdueBorrows() {
        return borrowService.getOverdueBorrows();
    }
}