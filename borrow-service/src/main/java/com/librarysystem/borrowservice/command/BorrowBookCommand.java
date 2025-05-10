package com.librarysystem.borrowservice.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

public class BorrowBookCommand {
  @TargetAggregateIdentifier
  private final String borrowId;
  private final Long userId;
  private final Long bookId;

  public BorrowBookCommand(String borrowId, Long userId, Long bookId) {
    this.borrowId = borrowId;
    this.userId = userId;
    this.bookId = bookId;
  }

  public String getBorrowId() { return borrowId; }
  public Long getUserId() { return userId; }
  public Long getBookId() { return bookId; }
}
