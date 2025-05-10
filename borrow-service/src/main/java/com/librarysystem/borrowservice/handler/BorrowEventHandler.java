package com.librarysystem.borrowservice.handler;

import com.librarysystem.borrowservice.event.BookBorrowedEvent;
import com.librarysystem.borrowservice.entity.Borrow;
import com.librarysystem.borrowservice.repository.BorrowRepository;
import lombok.RequiredArgsConstructor;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BorrowEventHandler {

  private final BorrowRepository borrowRepository;

  @EventHandler
  public void on(BookBorrowedEvent event) {
    Borrow borrow = new Borrow(event.getBorrowId(), event.getUserId(), event.getBookId());
    borrowRepository.save(borrow);
  }
}
