package com.librarysystem.borrowservice.service;

import com.librarysystem.borrowservice.entity.Borrow;
import com.librarysystem.borrowservice.repository.BorrowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * Service class for handling borrow-related business logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BorrowService {

    private final BorrowRepository borrowRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Helper to get the current authentication object from the security context.
     */
    private Authentication getAuth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * Helper to get the current user's ID from the authentication principal.
     */
    private String getCurrentUserId() {
        Authentication auth = getAuth();
        if (auth == null || auth.getPrincipal() == null) return null;
        return auth.getPrincipal().toString();
    }

    /**
     * Helper to check if the current user has a specific role.
     */
    private boolean hasRole(String role) {
        Authentication auth = getAuth();
        return auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_" + role));
    }

    /**
     * Handles the logic for borrowing a book.
     * Checks user permissions, creates a borrow record, and sends a Kafka event.
     */
    public ResponseEntity<String> borrowBook(Long bookId, String userId) {
        log.info("POST borrowBook called for " + userId);

        String currentUserId = getCurrentUserId();
        if (currentUserId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");

        boolean isPrivileged = hasRole("LIBRARIAN") || hasRole("ADMIN");
        if (!isPrivileged && !currentUserId.equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only borrow for yourself.");
        }

        String borrowId = UUID.randomUUID().toString();
        LocalDate borrowDate = LocalDate.now();
        LocalDate dueDate = borrowDate.plusDays(14);
        Borrow borrow = new Borrow(borrowId, userId, bookId, "PENDING", null, dueDate);
        borrowRepository.save(borrow);

        // Send event to Kafka for book reservation
        kafkaTemplate.send("book-reserve-requested", Map.of(
                "borrowId", borrowId,
                "bookId", bookId,
                "userId", userId
        ));
        return ResponseEntity.ok("Borrow request submitted: " + borrowId);
    }

    /**
     * Handles the logic for canceling a borrow request.
     * Checks permissions, updates borrow status, and sends a Kafka event if needed.
     */
    public ResponseEntity<String> cancelBorrow(String borrowId) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        boolean isPrivileged = hasRole("LIBRARIAN") || hasRole("ADMIN");

        return borrowRepository.findById(borrowId).map(borrow -> {
            if (!isPrivileged && !borrow.getUserId().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only cancel your own borrows.");
            }
            if ("RESERVED".equals(borrow.getStatus())) {
                borrow.setStatus("CANCELLED");
                borrowRepository.save(borrow);
                // Send compensation event to Kafka
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
     * Handles the logic for returning a borrowed book.
     * Checks permissions, updates borrow status, and sends a Kafka event.
     */
    public ResponseEntity<String> returnBook(String borrowId) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        boolean isPrivileged = hasRole("LIBRARIAN") || hasRole("ADMIN");

        return borrowRepository.findById(borrowId).map(borrow -> {
            if (!isPrivileged && !borrow.getUserId().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only return your own borrows.");
            }
            if ("RESERVED".equals(borrow.getStatus())) {
                borrow.setStatus("RETURNED");
                borrow.setReturnDate(LocalDate.now());
                borrowRepository.save(borrow);
                // Send event to Kafka for book return
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
     * Retrieves a specific borrow record.
     * Only privileged users or the owner can access the record.
     */
    public ResponseEntity<Borrow> getBorrow(String borrowId) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        boolean isPrivileged = hasRole("LIBRARIAN") || hasRole("ADMIN");

        return borrowRepository.findById(borrowId)
                .map(borrow -> {
                    if (isPrivileged || borrow.getUserId().equals(currentUserId)) {
                        return ResponseEntity.ok(borrow);
                    } else {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).<Borrow>build();
                    }
                })
                .orElse(ResponseEntity.<Borrow>notFound().build());
    }

    /**
     * Retrieves borrowing history for a user.
     * Privileged users can specify any user; others get their own history.
     */
    public ResponseEntity<List<Borrow>> getUserHistory(String userIdParam) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        boolean isPrivileged = hasRole("LIBRARIAN") || hasRole("ADMIN");

        String effectiveUserId = (isPrivileged && userIdParam != null) ? userIdParam : currentUserId;
        List<Borrow> borrows = borrowRepository.findByUserId(effectiveUserId);
        return ResponseEntity.ok(borrows);
    }

    /**
     * Retrieves borrowing history for all users.
     * Only accessible by privileged users.
     */
    public ResponseEntity<List<Borrow>> getAllHistory() {
        if (hasRole("LIBRARIAN") || hasRole("ADMIN")) {
            return ResponseEntity.ok(borrowRepository.findAll());
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    /**
     * Retrieves all overdue borrows (not returned and past due date).
     * Only accessible by privileged users.
     */
    public ResponseEntity<List<Borrow>> getOverdueBorrows() {
        if (hasRole("LIBRARIAN") || hasRole("ADMIN")) {
            LocalDate today = LocalDate.now();
            List<Borrow> overdue = borrowRepository.findByDueDateBeforeAndStatusNot(today, "RETURNED");
            return ResponseEntity.ok(overdue);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
}