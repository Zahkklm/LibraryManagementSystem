package com.librarysystem.bookservice.repository;

import com.librarysystem.bookservice.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Book entity persistence operations.
 * <p>
 * This repository provides data access methods for the Book domain entity
 * using Spring Data JPA. It handles all database operations including standard
 * CRUD operations inherited from JpaRepository as well as custom finder methods.
 * <p>
 * By extending JpaRepository, this interface automatically gets implementations for:
 * <ul>
 *   <li>save(Book) - Create or update a book</li>
 *   <li>findById(Long) - Find a book by its primary key</li>
 *   <li>findAll() - Retrieve all books</li>
 *   <li>deleteById(Long) - Delete a book by its ID</li>
 *   <li>existsById(Long) - Check if a book with given ID exists</li>
 *   <li>count() - Get the total number of books</li>
 *   <li>And many other standard repository methods</li>
 * </ul>
 * <p>
 * This repository is used by the BookService to perform data persistence operations
 * without having to interact directly with the database or write SQL queries.
 */
@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    
    /**
     * Finds a book by its ISBN (International Standard Book Number).
     * <p>
     * This method uses Spring Data JPA's method naming convention to automatically
     * generate a query that searches for books with a matching ISBN. This allows
     * the service layer to look up books by their industry-standard identifier.
     * <p>
     * The method returns an Optional which will be empty if no book with the
     * specified ISBN exists in the database.
     *
     * @param isbn The ISBN to search for (e.g., "978-3-16-148410-0")
     * @return An Optional containing the book if found, or empty if not found
     */
    Optional<Book> findByIsbn(String isbn);
    
    // TODO: Add findByTitleContaining, findByAuthor
    // Future enhancement: Add search capabilities for finding books by title or author
    // Examples:
    // List<Book> findByTitleContainingIgnoreCase(String titlePart);
    // List<Book> findByAuthorContainingIgnoreCase(String authorName);
}