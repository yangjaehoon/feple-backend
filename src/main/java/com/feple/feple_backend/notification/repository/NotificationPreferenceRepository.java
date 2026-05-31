package com.feple.feple_backend.notification.repository;

import com.feple.feple_backend.notification.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {
    @Query("SELECT np FROM NotificationPreference np WHERE np.userId = :userId")
    Optional<NotificationPreference> findByUserId(@Param("userId") Long userId);
}
