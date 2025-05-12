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
    List<Borrow> findByUserId(Long userId);
    List<Borrow> findByDueDateBeforeAndStatusNot(LocalDate date, String status);
}