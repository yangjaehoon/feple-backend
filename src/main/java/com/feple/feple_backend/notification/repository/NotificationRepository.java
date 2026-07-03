package com.feple.feple_backend.notification.repository;

import com.feple.feple_backend.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
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

    @Modifying
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.festival.id = :festivalId")
    void deleteByFestivalId(@Param("festivalId") Long festivalId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM notifications WHERE post_id IN :postIds", nativeQuery = true)
    void deleteByPostIdIn(@Param("postIds") List<Long> postIds);

    @Modifying
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.id = :id AND n.user.id = :userId")
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoff")
    void deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
