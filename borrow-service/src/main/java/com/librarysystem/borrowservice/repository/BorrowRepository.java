package com.librarysystem.borrowservice.repository;

import com.librarysystem.borrowservice.entity.Borrow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for managing Borrow entities.
 * Provides CRUD operations for borrow requests.
 */
@Repository
public interface BorrowRepository extends JpaRepository<Borrow, String> {
    /**
     * Finds all borrow records for a specific user.
     * @param userId the ID of the user (UUID as String)
     * @return list of Borrow entities for the user
     */
    List<Borrow> findByUserId(String userId); // Changed from Long to String

    /**
     * Finds all borrow records that are overdue (due date before the given date and not returned).
     * @param date the date to compare due dates against
     * @param status the status to exclude (e.g., "RETURNED")
     * @return list of overdue Borrow entities
     */
    List<Borrow> findByDueDateBeforeAndStatusNot(LocalDate date, String status);
}