package com.librarysystem.bookservice.service;

import com.librarysystem.bookservice.dto.BookResponse;
import com.librarysystem.bookservice.dto.CreateBookRequest;
import com.librarysystem.bookservice.dto.UpdateBookRequest;
import com.librarysystem.bookservice.entity.Book;
import com.librarysystem.bookservice.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    private Book sampleBook;
    private CreateBookRequest createBookRequest;
    private UpdateBookRequest updateBookRequest;

    @BeforeEach
    void setUp() {
        sampleBook = new Book();
        sampleBook.setId(1L);
        sampleBook.setTitle("Effective Java");
        sampleBook.setAuthor("Joshua Bloch");
        sampleBook.setIsbn("978-0134685991");
        sampleBook.setPublisher("Addison-Wesley");
        sampleBook.setPublicationDate(LocalDate.of(2018, 1, 6));
        sampleBook.setTotalCopies(10);
        sampleBook.setAvailableCopies(7);

        createBookRequest = new CreateBookRequest();
        createBookRequest.setTitle("Clean Code");
        createBookRequest.setAuthor("Robert C. Martin");
        createBookRequest.setIsbn("978-0132350884");
        createBookRequest.setPublisher("Prentice Hall");
        createBookRequest.setPublicationDate(LocalDate.of(2008, 8, 1));
        createBookRequest.setTotalCopies(5);
        createBookRequest.setAvailableCopies(5);

        updateBookRequest = new UpdateBookRequest();
        updateBookRequest.setTitle("Effective Java (3rd Edition)");
        updateBookRequest.setAvailableCopies(6);
    }

    @Test
    @DisplayName("addBook - Success")
    void addBook_shouldSaveAndReturnBookResponse() {
        when(bookRepository.findByIsbn(createBookRequest.getIsbn())).thenReturn(Optional.empty());
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> {
            Book bookToSave = invocation.getArgument(0);
            bookToSave.setId(2L); // Simulate ID generation
            return bookToSave;
        });

        BookResponse result = bookService.addBook(createBookRequest);

        assertNotNull(result);
        assertEquals(createBookRequest.getTitle(), result.getTitle());
        assertEquals(createBookRequest.getIsbn(), result.getIsbn());
        assertEquals(2L, result.getId());
        verify(bookRepository, times(1)).findByIsbn(createBookRequest.getIsbn());
        verify(bookRepository, times(1)).save(any(Book.class));
    }

    @Test
    @DisplayName("addBook - ISBN Already Exists")
    void addBook_shouldThrowException_whenIsbnExists() {
        when(bookRepository.findByIsbn(createBookRequest.getIsbn())).thenReturn(Optional.of(sampleBook));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bookService.addBook(createBookRequest));

        assertEquals("Book with ISBN " + createBookRequest.getIsbn() + " already exists.", exception.getMessage());
        verify(bookRepository, times(1)).findByIsbn(createBookRequest.getIsbn());
        verify(bookRepository, never()).save(any(Book.class));
    }
    
    @Test
    @DisplayName("addBook - Null AvailableCopies Defaults to TotalCopies")
    void addBook_shouldSetAvailableCopiesToTotalCopies_whenAvailableIsNull() {
        createBookRequest.setAvailableCopies(null);
        createBookRequest.setTotalCopies(10);
        when(bookRepository.findByIsbn(createBookRequest.getIsbn())).thenReturn(Optional.empty());
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookResponse result = bookService.addBook(createBookRequest);

        assertEquals(10, result.getAvailableCopies());
        assertEquals(10, result.getTotalCopies());
    }

    @Test
    @DisplayName("addBook - Null TotalCopies Defaults to AvailableCopies")
    void addBook_shouldSetTotalCopiesToAvailableCopies_whenTotalIsNull() {
        createBookRequest.setAvailableCopies(7);
        createBookRequest.setTotalCopies(null);
        when(bookRepository.findByIsbn(createBookRequest.getIsbn())).thenReturn(Optional.empty());
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookResponse result = bookService.addBook(createBookRequest);

        assertEquals(7, result.getAvailableCopies());
        assertEquals(7, result.getTotalCopies());
    }
    
    @Test
    @DisplayName("addBook - Both Copies Null Defaults to Zero")
    void addBook_shouldSetBothCopiesToZero_whenBothAreNull() {
        createBookRequest.setAvailableCopies(null);
        createBookRequest.setTotalCopies(null);
        when(bookRepository.findByIsbn(createBookRequest.getIsbn())).thenReturn(Optional.empty());
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookResponse result = bookService.addBook(createBookRequest);

        assertEquals(0, result.getAvailableCopies());
        assertEquals(0, result.getTotalCopies());
    }


    @Test
    @DisplayName("getBookById - Found")
    void getBookById_shouldReturnBookResponse_whenFound() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));

        Optional<BookResponse> result = bookService.getBookById(1L);

        assertTrue(result.isPresent());
        assertEquals(sampleBook.getTitle(), result.get().getTitle());
        verify(bookRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getBookById - Not Found")
    void getBookById_shouldReturnEmptyOptional_whenNotFound() {
        when(bookRepository.findById(anyLong())).thenReturn(Optional.empty());

        Optional<BookResponse> result = bookService.getBookById(99L);

        assertFalse(result.isPresent());
        verify(bookRepository, times(1)).findById(99L);
    }

    @Test
    @DisplayName("getBookByIsbn - Found")
    void getBookByIsbn_shouldReturnBookResponse_whenFound() {
        when(bookRepository.findByIsbn(sampleBook.getIsbn())).thenReturn(Optional.of(sampleBook));

        Optional<BookResponse> result = bookService.getBookByIsbn(sampleBook.getIsbn());

        assertTrue(result.isPresent());
        assertEquals(sampleBook.getTitle(), result.get().getTitle());
        verify(bookRepository, times(1)).findByIsbn(sampleBook.getIsbn());
    }

    @Test
    @DisplayName("getBookByIsbn - Not Found")
    void getBookByIsbn_shouldReturnEmptyOptional_whenNotFound() {
        when(bookRepository.findByIsbn(anyString())).thenReturn(Optional.empty());

        Optional<BookResponse> result = bookService.getBookByIsbn("000-0000000000");

        assertFalse(result.isPresent());
        verify(bookRepository, times(1)).findByIsbn("000-0000000000");
    }

    @Test
    @DisplayName("getAllBooks - Returns List")
    void getAllBooks_shouldReturnListOfBookResponses() {
        when(bookRepository.findAll()).thenReturn(List.of(sampleBook));

        List<BookResponse> results = bookService.getAllBooks();

        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        assertEquals(sampleBook.getTitle(), results.get(0).getTitle());
        verify(bookRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAllBooks - Returns Empty List")
    void getAllBooks_shouldReturnEmptyList_whenNoBooks() {
        when(bookRepository.findAll()).thenReturn(Collections.emptyList());

        List<BookResponse> results = bookService.getAllBooks();

        assertTrue(results.isEmpty());
        verify(bookRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("updateBook - Success")
    void updateBook_shouldUpdateAndReturnBookResponse() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookResponse result = bookService.updateBook(1L, updateBookRequest);

        assertNotNull(result);
        assertEquals(updateBookRequest.getTitle(), result.getTitle());
        assertEquals(updateBookRequest.getAvailableCopies(), result.getAvailableCopies());
        // Check that other fields remain unchanged if not in updateRequest
        assertEquals(sampleBook.getAuthor(), result.getAuthor());
        verify(bookRepository, times(1)).findById(1L);
        verify(bookRepository, times(1)).save(any(Book.class));
    }

    @Test
    @DisplayName("updateBook - Book Not Found")
    void updateBook_shouldThrowException_whenBookNotFound() {
        when(bookRepository.findById(anyLong())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> bookService.updateBook(99L, updateBookRequest));

        assertEquals("Book not found with id: 99", exception.getMessage());
        verify(bookRepository, times(1)).findById(99L);
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    @DisplayName("updateBook - ISBN Conflict")
    void updateBook_shouldThrowException_whenIsbnConflict() {
        UpdateBookRequest conflictingUpdateRequest = new UpdateBookRequest();
        conflictingUpdateRequest.setIsbn("978-0132350884"); // ISBN of a different book

        Book existingBookWithNewIsbn = new Book();
        existingBookWithNewIsbn.setId(3L);
        existingBookWithNewIsbn.setIsbn("978-0132350884");

        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook)); // sampleBook has ISBN "978-0134685991"
        when(bookRepository.findByIsbn("978-0132350884")).thenReturn(Optional.of(existingBookWithNewIsbn));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bookService.updateBook(1L, conflictingUpdateRequest));

        assertEquals("Another book with ISBN " + conflictingUpdateRequest.getIsbn() + " already exists.", exception.getMessage());
        verify(bookRepository, times(1)).findById(1L);
        verify(bookRepository, times(1)).findByIsbn("978-0132350884");
        verify(bookRepository, never()).save(any(Book.class));
    }
    
    @Test
    @DisplayName("updateBook - Update ISBN Successfully")
    void updateBook_shouldUpdateIsbn_whenNoConflict() {
        UpdateBookRequest isbnUpdateRequest = new UpdateBookRequest();
        String newIsbn = "000-NEWISBN000";
        isbnUpdateRequest.setIsbn(newIsbn);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));
        when(bookRepository.findByIsbn(newIsbn)).thenReturn(Optional.empty()); // No conflict
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookResponse result = bookService.updateBook(1L, isbnUpdateRequest);

        assertEquals(newIsbn, result.getIsbn());
        verify(bookRepository, times(1)).save(any(Book.class));
    }


    @Test
    @DisplayName("deleteBook - Success")
    void deleteBook_shouldCallRepositoryDelete() {
        when(bookRepository.existsById(1L)).thenReturn(true);
        doNothing().when(bookRepository).deleteById(1L);

        assertDoesNotThrow(() -> bookService.deleteBook(1L));

        verify(bookRepository, times(1)).existsById(1L);
        verify(bookRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("deleteBook - Book Not Found")
    void deleteBook_shouldThrowException_whenBookNotFound() {
        when(bookRepository.existsById(anyLong())).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> bookService.deleteBook(99L));

        assertEquals("Book not found with id: 99", exception.getMessage());
        verify(bookRepository, times(1)).existsById(99L);
        verify(bookRepository, never()).deleteById(anyLong());
    }
}