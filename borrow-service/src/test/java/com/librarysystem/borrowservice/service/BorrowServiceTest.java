package com.librarysystem.borrowservice.service;

import com.librarysystem.borrowservice.entity.Borrow;
import com.librarysystem.borrowservice.repository.BorrowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class BorrowServiceTest {

    @Mock
    private BorrowRepository borrowRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private BorrowService borrowService;

    private Borrow sampleBorrow;

    private static final String USER_ID = "user123";
    private static final String BORROW_ID = "borrow-xyz";
    private static final Long BOOK_ID = 42L;

    @BeforeEach
    void setUp() {
        // Example Borrow record
        sampleBorrow = new Borrow(
                BORROW_ID,
                USER_ID,
                BOOK_ID,
                "RESERVED",
                null,
                LocalDate.now().plusDays(14)
        );

        // Default security context setup
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private void mockUser(String principalId, String role) {
        Collection<? extends GrantedAuthority> authorities =
                Collections.singletonList((GrantedAuthority) () -> "ROLE_" + role);

        doReturn(principalId).when(authentication).getPrincipal();
        doReturn(authorities).when(authentication).getAuthorities(); // <-- Important
    }

    @Test
    @DisplayName("borrowBook() - success for valid user")
    void testBorrowBookSuccess() {
        mockUser(USER_ID, "USER");
        when(borrowRepository.save(any(Borrow.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<String> response = borrowService.borrowBook(BOOK_ID, USER_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Borrow request submitted:"));
        verify(borrowRepository).save(any(Borrow.class));
        verify(kafkaTemplate).send(eq("book-reserve-requested"), anyMap());
    }

    @Test
    @DisplayName("borrowBook() - returns 401 when no user authenticated")
    void testBorrowBookUnauthorized() {
        when(securityContext.getAuthentication()).thenReturn(null);

        ResponseEntity<String> response = borrowService.borrowBook(BOOK_ID, USER_ID);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Unauthorized", response.getBody());
        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    @Test
    @DisplayName("cancelBorrow() - success cancels RESERVED borrow")
    void testCancelBorrowSuccess() {
        mockUser(USER_ID, "USER");
        when(borrowRepository.findById(BORROW_ID)).thenReturn(Optional.of(sampleBorrow));
        when(borrowRepository.save(any(Borrow.class))).thenReturn(sampleBorrow);

        ResponseEntity<String> response = borrowService.cancelBorrow(BORROW_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Borrow cancelled and compensation event sent.", response.getBody());
        verify(kafkaTemplate).send(eq("book-reservation-cancelled"), anyMap());
    }

    @Test
    @DisplayName("cancelBorrow() - returns Not Found when borrow doesn't exist")
    void testCancelBorrowNotFound() {
        mockUser(USER_ID, "USER");
        when(borrowRepository.findById(BORROW_ID)).thenReturn(Optional.empty());

        ResponseEntity<String> response = borrowService.cancelBorrow(BORROW_ID);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("returnBook() - success returns RESERVED book")
    void testReturnBookSuccess() {
        mockUser(USER_ID, "USER");
        when(borrowRepository.findById(BORROW_ID)).thenReturn(Optional.of(sampleBorrow));
        when(borrowRepository.save(any(Borrow.class))).thenReturn(sampleBorrow);

        ResponseEntity<String> response = borrowService.returnBook(BORROW_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Book returned and event sent.", response.getBody());
        verify(kafkaTemplate).send(eq("book-returned"), anyMap());
    }

    @Test
    @DisplayName("getBorrow() - success for owner's borrow record")
    void testGetBorrowSuccess() {
        mockUser(USER_ID, "USER");
        when(borrowRepository.findById(BORROW_ID)).thenReturn(Optional.of(sampleBorrow));

        ResponseEntity<Borrow> response = borrowService.getBorrow(BORROW_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(sampleBorrow, response.getBody());
    }

    @Test
    @DisplayName("getUserHistory() - returns a user's borrow history")
    void testGetUserHistory() {
        mockUser(USER_ID, "USER");
        when(borrowRepository.findByUserId(USER_ID))
                .thenReturn(Collections.singletonList(sampleBorrow));

        ResponseEntity<List<Borrow>> response = borrowService.getUserHistory(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(sampleBorrow, response.getBody().get(0));
    }

    @Test
    @DisplayName("getAllHistory() - accessible by LIBRARIAN or ADMIN")
    void testGetAllHistoryAuthorized() {
        mockUser("admin1", "ADMIN");
        when(borrowRepository.findAll()).thenReturn(Collections.singletonList(sampleBorrow));

        ResponseEntity<List<Borrow>> response = borrowService.getAllHistory();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains(sampleBorrow));
    }

    @Test
    @DisplayName("getOverdueBorrows() - accessible by LIBRARIAN or ADMIN")
    void testGetOverdueBorrows() {
        mockUser("librarian1", "LIBRARIAN");
        when(borrowRepository.findByDueDateBeforeAndStatusNot(any(LocalDate.class), eq("RETURNED")))
                .thenReturn(Collections.singletonList(sampleBorrow));

        ResponseEntity<List<Borrow>> response = borrowService.getOverdueBorrows();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(sampleBorrow, response.getBody().get(0));
    }
}
