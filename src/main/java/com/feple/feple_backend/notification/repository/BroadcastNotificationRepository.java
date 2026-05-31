package com.feple.feple_backend.notification.repository;

import com.feple.feple_backend.notification.entity.BroadcastNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BroadcastNotificationRepository extends JpaRepository<BroadcastNotification, Long> {
    List<BroadcastNotification> findAllByOrderByCreatedAtDesc();
}
