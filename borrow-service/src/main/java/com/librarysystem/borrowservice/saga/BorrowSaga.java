package com.librarysystem.borrowservice.saga;

import com.librarysystem.borrowservice.event.BookBorrowedEvent;
import lombok.extern.slf4j.Slf4j;

import com.librarysystem.borrowservice.event.BookBorrowedEvent;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.modelling.saga.*;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Saga
public class BorrowSaga {
  
  @Transient
  @Autowired
  private transient CommandGateway commandGateway;

  @StartSaga
  @SagaEventHandler(associationProperty = "borrowId")
  public void on(BookBorrowedEvent event) {
    log.info("BorrowSaga started for borrowId: {}", event.getBorrowId());
    // 1. Send ReserveBookCommand to book-service (via Axon or REST)
    // commandGateway.send(new ReserveBookCommand(event.getBookId(), event.getBorrowId()));
  }

  // 2. Handle BookReservedEvent, send ValidateUserCommand, etc.
  // 3. Handle UserValidatedEvent, finalize borrow, end saga
  // 4. Handle failures and compensations
}
