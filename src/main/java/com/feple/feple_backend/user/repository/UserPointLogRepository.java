package com.feple.feple_backend.user.repository;

import com.feple.feple_backend.user.entity.UserPointLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserPointLogRepository extends JpaRepository<UserPointLog, Long> {

    @Query("SELECT p FROM UserPointLog p WHERE p.user.id = :userId ORDER BY p.createdAt DESC")
    Page<UserPointLog> findByUserId(@Param("userId") Long userId, Pageable pageable);

    // 회원 완전 삭제(hardDelete) 시 users 행 물리 삭제 전에 남은 포인트 로그를 비운다 (FK RESTRICT).
    @Modifying
    @Transactional
    @Query("DELETE FROM UserPointLog p WHERE p.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Query("SELECT p FROM UserPointLog p JOIN FETCH p.user ORDER BY p.createdAt DESC")
    Page<UserPointLog> findAllWithUser(Pageable pageable);

    @Query("SELECT p FROM UserPointLog p JOIN FETCH p.user u " +
           "WHERE LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '!' " +
           "ORDER BY p.createdAt DESC")
    Page<UserPointLog> searchByUserKeyword(@Param("keyword") String keyword, Pageable pageable);
}
