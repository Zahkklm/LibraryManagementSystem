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

@Service
@RequiredArgsConstructor
@Slf4j
public class BorrowService {

    private final BorrowRepository borrowRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private Authentication getAuth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private String getCurrentUserId() {
        Authentication auth = getAuth();
        if (auth == null || auth.getPrincipal() == null) return null;
        return auth.getPrincipal().toString();
    }

    private boolean hasRole(String role) {
        Authentication auth = getAuth();
        return auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_" + role));
    }

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

        kafkaTemplate.send("book-reserve-requested", Map.of(
                "borrowId", borrowId,
                "bookId", bookId,
                "userId", userId
        ));
        return ResponseEntity.ok("Borrow request submitted: " + borrowId);
    }

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

    public ResponseEntity<List<Borrow>> getUserHistory(String userIdParam) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        boolean isPrivileged = hasRole("LIBRARIAN") || hasRole("ADMIN");

        String effectiveUserId = (isPrivileged && userIdParam != null) ? userIdParam : currentUserId;
        List<Borrow> borrows = borrowRepository.findByUserId(effectiveUserId);
        return ResponseEntity.ok(borrows);
    }

    public ResponseEntity<List<Borrow>> getAllHistory() {
        if (hasRole("LIBRARIAN") || hasRole("ADMIN")) {
            return ResponseEntity.ok(borrowRepository.findAll());
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    public ResponseEntity<List<Borrow>> getOverdueBorrows() {
        if (hasRole("LIBRARIAN") || hasRole("ADMIN")) {
            LocalDate today = LocalDate.now();
            List<Borrow> overdue = borrowRepository.findByDueDateBeforeAndStatusNot(today, "RETURNED");
            return ResponseEntity.ok(overdue);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
}