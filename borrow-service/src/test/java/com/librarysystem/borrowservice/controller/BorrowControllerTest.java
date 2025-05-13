package com.librarysystem.borrowservice.controller;

import com.librarysystem.borrowservice.entity.Borrow;
import com.librarysystem.borrowservice.service.BorrowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BorrowControllerTest {

    @Mock
    private BorrowService borrowService;

    @InjectMocks
    private BorrowController borrowController;

    private static final String BORROW_ID = "borrow-xyz";
    private static final String USER_ID = "user123";
    private static final Long BOOK_ID = 42L;
    private Borrow sampleBorrow;

    @BeforeEach
    void setUp() {
        sampleBorrow = new Borrow(BORROW_ID, USER_ID, BOOK_ID, "RESERVED", null, null);
    }

    @Test
    @DisplayName("borrowBook() - success")
    void testBorrowBookSuccess() {
        when(borrowService.borrowBook(BOOK_ID, USER_ID))
                .thenReturn(ResponseEntity.ok("Borrow request submitted: " + BORROW_ID));

        ResponseEntity<String> response = borrowController.borrowBook(BOOK_ID, USER_ID);

        // Changed to compare using HttpStatus.OK
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Borrow request submitted:"));
    }

    @Test
    @DisplayName("cancelBorrow() - success")
    void testCancelBorrowSuccess() {
        when(borrowService.cancelBorrow(BORROW_ID))
                .thenReturn(ResponseEntity.ok("Borrow cancelled and compensation event sent."));

        ResponseEntity<String> response = borrowController.cancelBorrow(BORROW_ID);

        // Changed to compare using HttpStatus.OK
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Borrow cancelled and compensation event sent.", response.getBody());
    }

    @Test
    @DisplayName("returnBook() - success")
    void testReturnBookSuccess() {
        when(borrowService.returnBook(BORROW_ID))
                .thenReturn(ResponseEntity.ok("Book returned and event sent."));

        ResponseEntity<String> response = borrowController.returnBook(BORROW_ID);

        // Changed to compare using HttpStatus.OK
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Book returned and event sent.", response.getBody());
    }

    @Test
    @DisplayName("getBorrow() - success")
    void testGetBorrowSuccess() {
        when(borrowService.getBorrow(BORROW_ID))
                .thenReturn(ResponseEntity.ok(sampleBorrow));

        ResponseEntity<Borrow> response = borrowController.getBorrow(BORROW_ID);

        // Changed to compare using HttpStatus.OK
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(sampleBorrow, response.getBody());
    }

    @Test
    @DisplayName("getUserHistory() - success")
    void testGetUserHistory() {
        when(borrowService.getUserHistory(null))
                .thenReturn(ResponseEntity.ok(Collections.singletonList(sampleBorrow)));

        ResponseEntity<List<Borrow>> response = borrowController.getUserHistory(null);

        // Changed to compare using HttpStatus.OK
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(sampleBorrow, response.getBody().get(0));
    }

    @Test
    @DisplayName("getAllHistory() - success")
    void testGetAllHistory() {
        when(borrowService.getAllHistory())
                .thenReturn(ResponseEntity.ok(Collections.singletonList(sampleBorrow)));

        ResponseEntity<List<Borrow>> response = borrowController.getAllHistory();

        // Changed to compare using HttpStatus.OK
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(sampleBorrow, response.getBody().get(0));
    }

    @Test
    @DisplayName("getOverdueBorrows() - success")
    void testGetOverdueBorrows() {
        when(borrowService.getOverdueBorrows())
                .thenReturn(ResponseEntity.ok(Collections.singletonList(sampleBorrow)));

        ResponseEntity<List<Borrow>> response = borrowController.getOverdueBorrows();

        // Changed to compare using HttpStatus.OK
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(sampleBorrow, response.getBody().get(0));
    }
}