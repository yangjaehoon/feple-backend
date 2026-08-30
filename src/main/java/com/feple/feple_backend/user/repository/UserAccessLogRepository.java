package com.feple.feple_backend.user.repository;

import com.feple.feple_backend.user.entity.UserAccessLog;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserAccessLogRepository extends JpaRepository<UserAccessLog, Long> {

    // UNIQUE(user_id, access_date) 제약 하에서 이미 오늘 기록된 사용자는 조용히 무시 —
    // 동시 요청 간 경쟁 상태에서도 중복 삽입 없이 안전하게 동작한다.
    @Modifying
    @Transactional
    @Query(value = "INSERT IGNORE INTO user_access_log (user_id, access_date, created_at) VALUES (:userId, :accessDate, :now)",
            nativeQuery = true)
    void insertIgnore(@Param("userId") Long userId, @Param("accessDate") LocalDate accessDate, @Param("now") LocalDateTime now);

    long countByAccessDate(LocalDate accessDate);

    @Query(value = "SELECT COUNT(DISTINCT user_id) FROM user_access_log WHERE access_date BETWEEN :from AND :to",
            nativeQuery = true)
    Long countDistinctUsersBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    // 한 번에 전체를 지우면 하나의 커넥션·트랜잭션을 오래 붙잡는다 — LIMIT으로 잘라 여러 번 커밋
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM user_access_log WHERE access_date < :cutoff LIMIT :batchSize", nativeQuery = true)
    int deleteByAccessDateBeforeBatch(@Param("cutoff") LocalDate cutoff, @Param("batchSize") int batchSize);

    // 회원 완전 삭제(hard delete) 전용 — 소프트 삭제 캐스케이드가 다루지 않는 접속 로그를 정리한다.
    @Modifying
    @Query(value = "DELETE FROM user_access_log WHERE user_id = :userId", nativeQuery = true)
    void deleteByUserId(@Param("userId") Long userId);
}
