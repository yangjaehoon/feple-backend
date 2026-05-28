package com.feple.feple_backend.admin.log;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AdminLogRepository extends JpaRepository<AdminLog, Long> {
    Page<AdminLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<AdminLog> findByTargetTypeOrderByCreatedAtDesc(String targetType, Pageable pageable);
    List<AdminLog> findTop10ByOrderByCreatedAtDesc();

    @Query("SELECT l FROM AdminLog l WHERE l.createdAt BETWEEN :from AND :to ORDER BY l.createdAt DESC")
    Page<AdminLog> findByDateRange(@Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to,
                                   Pageable pageable);

    @Query("SELECT l FROM AdminLog l WHERE l.targetType = :type AND l.createdAt BETWEEN :from AND :to ORDER BY l.createdAt DESC")
    Page<AdminLog> findByTargetTypeAndDateRange(@Param("type") String type,
                                                @Param("from") LocalDateTime from,
                                                @Param("to") LocalDateTime to,
                                                Pageable pageable);
}
