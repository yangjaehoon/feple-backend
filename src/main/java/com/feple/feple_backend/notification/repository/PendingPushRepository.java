package com.feple.feple_backend.notification.repository;

import com.feple.feple_backend.notification.entity.PendingPush;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PendingPushRepository extends JpaRepository<PendingPush, Long> {
}
