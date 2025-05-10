package com.librarysystem.borrowservice.controller;

import com.librarysystem.borrowservice.command.BorrowBookCommand;
import lombok.RequiredArgsConstructor;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/borrow")
@RequiredArgsConstructor
public class BorrowController {

  private final CommandGateway commandGateway;

  @PostMapping
  public ResponseEntity<String> borrowBook(@RequestParam Long userId, @RequestParam Long bookId) {
    String borrowId = UUID.randomUUID().toString();
    BorrowBookCommand command = new BorrowBookCommand(borrowId, userId, bookId);
    commandGateway.sendAndWait(command);
    return ResponseEntity.ok("Book borrowed successfully with ID: " + borrowId);
  }
}

