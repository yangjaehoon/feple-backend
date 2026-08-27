package com.feple.feple_backend.notification.repository;

import com.feple.feple_backend.notification.entity.BroadcastNotification;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BroadcastNotificationRepository extends JpaRepository<BroadcastNotification, Long> {
    List<BroadcastNotification> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
