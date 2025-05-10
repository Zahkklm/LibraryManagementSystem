package com.librarysystem.borrowservice.event;

public class BookBorrowedEvent {
  private final String borrowId;
  private final Long userId;
  private final Long bookId;

  public BookBorrowedEvent(String borrowId, Long userId, Long bookId) {
    this.borrowId = borrowId;
    this.userId = userId;
    this.bookId = bookId;
  }

  public String getBorrowId() { return borrowId; }
  public Long getUserId() { return userId; }
  public Long getBookId() { return bookId; }
}
