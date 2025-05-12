package com.librarysystem.bookservice.saga;

import com.librarysystem.bookservice.entity.Book;
import com.librarysystem.bookservice.repository.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Saga event listener for handling book reservation requests.
 *
 * Listens for book-reserve-requested events from the borrow-service,
 * checks book availability, updates inventory, and publishes the result
 * to either the book-reserved or book-reserve-failed topic.
 */
@Component
public class BookSagaListener {
    private static final Logger logger = LoggerFactory.getLogger(BookSagaListener.class);

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    private BookRepository bookRepository;

    /**
     * Handles book reservation requests.
     * Consumes events from the "book-reserve-requested" topic, checks if the book is available,
     * updates the available copies, and publishes the reservation result.
     *
     * @param event the event data containing borrowId, bookId, and userId
     */
    @KafkaListener(topics = "book-reserve-requested", groupId = "book-saga")
    public void handleBookReserveRequested(Map<String, Object> event) {
        logger.info("Received book-reserve-requested event: {}", event);

        String borrowId = (String) event.get("borrowId");
        Long bookId = event.get("bookId") != null ? ((Number) event.get("bookId")).longValue() : null;
        Long userId = event.get("userId") != null ? ((Number) event.get("userId")).longValue() : null;

        if (bookId == null) {
            logger.warn("Book ID is missing in event: {}", event);
            // Attempt to send a failed reservation event if borrowId is present
            if (borrowId != null) {
                kafkaTemplate.send("book-reserve-failed", Map.of(
                    "borrowId", borrowId
                ));
                logger.info("Sent book-reserve-failed event due to missing bookId for borrowId: {}", borrowId);
            }
            return;
        }

        Optional<Book> bookOpt = bookRepository.findById(bookId);
        if (bookOpt.isPresent() && bookOpt.get().getAvailableCopies() > 0) {
            Book book = bookOpt.get();
            book.setAvailableCopies(book.getAvailableCopies() - 1);
            bookRepository.save(book);

            // Publish a successful reservation event
            kafkaTemplate.send("book-reserved", Map.of(
                "borrowId", borrowId,
                "bookId", bookId
            ));
            logger.info("Book reserved successfully. Book ID: {}, Remaining copies: {}", bookId, book.getAvailableCopies());
        } else {
            // Publish a failed reservation event
            kafkaTemplate.send("book-reserve-failed", Map.of(
                "borrowId", borrowId
            ));
            logger.info("Book reservation failed. Book ID: {} not available or no copies left.", bookId);
        }
    }

    /**
     * Handles compensation event for cancelled reservations.
     * Increments available copies when a borrow is cancelled.
     *
     * @param event the event data containing borrowId and bookId
     */
    @KafkaListener(topics = "book-reservation-cancelled", groupId = "book-saga")
    public void handleBookReservationCancelled(Map<String, Object> event) {
        Long bookId = event.get("bookId") != null ? ((Number) event.get("bookId")).longValue() : null;
        if (bookId == null) {
            logger.warn("Book ID is missing in compensation event: {}", event);
            return;
        }
        bookRepository.findById(bookId).ifPresent(book -> {
            book.setAvailableCopies(book.getAvailableCopies() + 1);
            bookRepository.save(book);
            logger.info("Compensation: Incremented available copies for book ID: {}", bookId);
        });
    }

    /**
     * Handles book return events.
     * Increments available copies when a book is returned.
     *
     * @param event the event data containing borrowId and bookId
     */
    @KafkaListener(topics = "book-returned", groupId = "book-saga")
    public void handleBookReturned(Map<String, Object> event) {
        Long bookId = event.get("bookId") != null ? ((Number) event.get("bookId")).longValue() : null;
        if (bookId == null) {
            logger.warn("Book ID is missing in return event: {}", event);
            return;
        }
        bookRepository.findById(bookId).ifPresent(book -> {
            book.setAvailableCopies(book.getAvailableCopies() + 1);
            bookRepository.save(book);
            logger.info("Book returned: Incremented available copies for book ID: {}", bookId);
        });
    }
}