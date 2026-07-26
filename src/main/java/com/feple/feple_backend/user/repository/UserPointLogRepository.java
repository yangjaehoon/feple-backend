package com.feple.feple_backend.user.repository;

import com.feple.feple_backend.user.entity.UserPointLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserPointLogRepository extends JpaRepository<UserPointLog, Long> {

    @Query("SELECT p FROM UserPointLog p WHERE p.user.id = :userId ORDER BY p.createdAt DESC")
    Page<UserPointLog> findByUserId(@Param("userId") Long userId, Pageable pageable);
}
