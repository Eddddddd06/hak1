package com.example.hack1.repository;

import com.oreo.insight.domain.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface SaleRepository extends JpaRepository<Sale, String> {

    @Query("SELECT s FROM Sale s WHERE (:branch IS NULL OR s.branch = :branch) AND s.soldAt BETWEEN :from AND :to")
    Page<Sale> findByFilters(
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("branch") String branch,
            Pageable pageable);

    @Query("SELECT s FROM Sale s WHERE s.soldAt BETWEEN :from AND :to")
    Page<Sale> findByDateRange(
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}