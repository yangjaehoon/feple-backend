package com.feple.feple_backend.admin.log;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AdminLogRepository extends JpaRepository<AdminLog, Long> {
    List<AdminLog> findTop10ByOrderByCreatedAtDesc();

    @Query("SELECT l FROM AdminLog l WHERE " +
           "(:type IS NULL OR l.targetType = :type) AND " +
           "(:username IS NULL OR LOWER(l.adminUsername) LIKE LOWER(CONCAT('%', :username, '%')) ESCAPE '!') AND " +
           "(:from IS NULL OR l.createdAt >= :from) AND " +
           "(:to IS NULL OR l.createdAt <= :to) " +
           "ORDER BY l.createdAt DESC")
    Page<AdminLog> findWithFilters(@Param("type") String type,
                                   @Param("username") String username,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to,
                                   Pageable pageable);
}
