package com.librarysystem.borrowservice.handler;

import com.librarysystem.borrowservice.command.BorrowBookCommand;
import com.librarysystem.borrowservice.event.BookBorrowedEvent;
import com.librarysystem.borrowservice.entity.Borrow;
import com.librarysystem.borrowservice.repository.BorrowRepository;
import lombok.RequiredArgsConstructor;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Aggregate
@RequiredArgsConstructor
public class BorrowCommandHandler {

  @AggregateIdentifier
  private String borrowId;

  private Long userId;
  private Long bookId;

  @CommandHandler
  public BorrowCommandHandler(BorrowBookCommand command) {
    apply(new BookBorrowedEvent(command.getBorrowId(), command.getUserId(), command.getBookId()));
  }

  @EventSourcingHandler
  public void on(BookBorrowedEvent event) {
    this.borrowId = event.getBorrowId();
    this.userId = event.getUserId();
    this.bookId = event.getBookId();
  }
}
