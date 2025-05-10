package com.librarysystem.borrowservice.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Borrow {

  @Id
  private String id;
  private Long userId;
  private Long bookId;
  private LocalDate borrowDate;
  private LocalDate dueDate;

  public Borrow() {}

  public Borrow(String id, Long userId, Long bookId) {
    this.id = id;
    this.userId = userId;
    this.bookId = bookId;
    this.borrowDate = LocalDate.now();
    this.dueDate = borrowDate.plusDays(14);
  }

  // Getters and setters
}
