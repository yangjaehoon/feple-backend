package com.feple.feple_backend.notification.repository;

import com.feple.feple_backend.notification.entity.Notification;
import com.feple.feple_backend.notification.entity.NotificationType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
            SELECT n FROM Notification n
            LEFT JOIN FETCH n.festival
            LEFT JOIN FETCH n.artist
            LEFT JOIN FETCH n.post np
            LEFT JOIN FETCH np.festival
            WHERE n.user.id = :userId
            ORDER BY n.createdAt DESC
            """)
    List<Notification> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    // 타입 필터가 있을 때는 최신 N건을 먼저 자른 뒤 타입으로 걸러내면(findByUserIdOrderByCreatedAtDesc +
    // 인메모리 필터) 최신 N건이 특정 타입에 편중된 경우 결과가 실제보다 적게 나올 수 있다 —
    // 타입 조건을 쿼리 단계에서 먼저 적용해 이 문제를 근본적으로 없앤다.
    @Query("""
            SELECT n FROM Notification n
            LEFT JOIN FETCH n.festival
            LEFT JOIN FETCH n.artist
            LEFT JOIN FETCH n.post np
            LEFT JOIN FETCH np.festival
            WHERE n.user.id = :userId AND n.type IN :types
            ORDER BY n.createdAt DESC
            """)
    List<Notification> findByUserIdAndTypeInOrderByCreatedAtDesc(
            @Param("userId") Long userId, @Param("types") Set<NotificationType> types, Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.isRead = false")
    long countByUserIdAndIsReadFalse(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    int markAllReadByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    // 회원 완전 삭제(hardDelete) — 이 유저의 글을 참조하는 알림(수신자 무관)을 물리 삭제해
    // notifications.post_id FK가 post 물리 삭제를 막지 않도록 한다.
    @Modifying
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.post.id IN :postIds")
    void deleteByPostIds(@Param("postIds") List<Long> postIds);

    // 한 번에 전체를 지우면 하나의 커넥션·트랜잭션을 오래 붙잡는다 — LIMIT으로 잘라 여러 번 커밋
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM notifications WHERE created_at < :cutoff LIMIT :batchSize", nativeQuery = true)
    int deleteOlderThanBatch(@Param("cutoff") LocalDateTime cutoff, @Param("batchSize") int batchSize);
}
