package com.librarysystem.borrowservice.controller;

import com.librarysystem.borrowservice.entity.Borrow;
import com.librarysystem.borrowservice.repository.BorrowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.time.LocalDate;
import java.util.Map;
import java.util.List;

/**
 * REST controller for handling borrow requests.
 * User ID and roles are extracted from the Spring Security context (populated by RoleExtractionFilter).
 */
@RestController
@RequestMapping("/api/borrows")
@RequiredArgsConstructor
public class BorrowController {
    private final BorrowRepository borrowRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private Authentication getAuth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private Long getCurrentUserId() {
        Authentication auth = getAuth();
        if (auth == null || auth.getPrincipal() == null) return null;
        try {
            return Long.valueOf(auth.getPrincipal().toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean hasRole(String role) {
        Authentication auth = getAuth();
        return auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_" + role));
    }

    /**
     * Create a new borrow request.
     * User ID is always taken from the authenticated principal.
     */
    @PostMapping
    public ResponseEntity<String> borrowBook(@RequestParam Long bookId) {
        Long userId = getCurrentUserId();
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");

        String borrowId = UUID.randomUUID().toString();
        LocalDate borrowDate = LocalDate.now();
        LocalDate dueDate = borrowDate.plusDays(14);
        Borrow borrow = new Borrow(borrowId, userId, bookId, "PENDING", null, dueDate);
        borrowRepository.save(borrow);

        kafkaTemplate.send("book-reserve-requested", Map.of(
            "borrowId", borrowId,
            "bookId", bookId,
            "userId", userId
        ));
        return ResponseEntity.ok("Borrow request submitted: " + borrowId);
    }

    /**
     * Cancel a borrow request (compensating action).
     * Only the owner (MEMBER) or LIBRARIAN/ADMIN can cancel.
     */
    @PostMapping("/{borrowId}/cancel")
    public ResponseEntity<String> cancelBorrow(@PathVariable String borrowId) {
        Long userId = getCurrentUserId();
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        boolean isPrivileged = hasRole("LIBRARIAN") || hasRole("ADMIN");

        return borrowRepository.findById(borrowId).map(borrow -> {
            if (!isPrivileged && !borrow.getUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only cancel your own borrows.");
            }
            if ("RESERVED".equals(borrow.getStatus())) {
                borrow.setStatus("CANCELLED");
                borrowRepository.save(borrow);
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
     * Return a borrowed book.
     * Only the owner (MEMBER) or LIBRARIAN/ADMIN can return.
     */
    @PostMapping("/{borrowId}/return")
    public ResponseEntity<String> returnBook(@PathVariable String borrowId) {
        Long userId = getCurrentUserId();
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        boolean isPrivileged = hasRole("LIBRARIAN") || hasRole("ADMIN");

        return borrowRepository.findById(borrowId).map(borrow -> {
            if (!isPrivileged && !borrow.getUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only return your own borrows.");
            }
            if ("RESERVED".equals(borrow.getStatus())) {
                borrow.setStatus("RETURNED");
                borrow.setReturnDate(LocalDate.now());
                borrowRepository.save(borrow);
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

    /**
     * Get a single borrow by ID.
     * MEMBER can only view their own borrows. LIBRARIAN/ADMIN can view any.
     */
    @GetMapping("/{borrowId}")
    public ResponseEntity<Borrow> getBorrow(@PathVariable String borrowId) {
        Long userId = getCurrentUserId();
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        boolean isPrivileged = hasRole("LIBRARIAN") || hasRole("ADMIN");

        return borrowRepository.findById(borrowId)
                .map(borrow -> {
                    if (isPrivileged || borrow.getUserId().equals(userId)) {
                        return ResponseEntity.ok(borrow);
                    } else {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).<Borrow>build();
                    }
                })
                .orElse(ResponseEntity.<Borrow>notFound().build());
    }

    /**
     * Get current user's borrowing history.
     * MEMBER always gets their own history. LIBRARIAN/ADMIN can query any user.
     */
    @GetMapping("/history")
    public ResponseEntity<List<Borrow>> getUserHistory(
            @RequestParam(value = "userId", required = false) Long userIdParam) {
        Long userId = getCurrentUserId();
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        boolean isPrivileged = hasRole("LIBRARIAN") || hasRole("ADMIN");

        if (isPrivileged && userIdParam != null) {
            List<Borrow> borrows = borrowRepository.findByUserId(userIdParam);
            return ResponseEntity.ok(borrows);
        } else {
            List<Borrow> borrows = borrowRepository.findByUserId(userId);
            return ResponseEntity.ok(borrows);
        }
    }

    /**
     * Get all borrowing history (admin/librarian only).
     */
    @GetMapping("/history/all")
    public ResponseEntity<List<Borrow>> getAllHistory() {
        if (hasRole("LIBRARIAN") || hasRole("ADMIN")) {
            return ResponseEntity.ok(borrowRepository.findAll());
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    /**
     * Get all overdue borrows (admin/librarian only).
     */
    @GetMapping("/overdue")
    public ResponseEntity<List<Borrow>> getOverdueBorrows() {
        if (hasRole("LIBRARIAN") || hasRole("ADMIN")) {
            LocalDate today = LocalDate.now();
            List<Borrow> overdue = borrowRepository.findByDueDateBeforeAndStatusNot(today, "RETURNED");
            return ResponseEntity.ok(overdue);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
}