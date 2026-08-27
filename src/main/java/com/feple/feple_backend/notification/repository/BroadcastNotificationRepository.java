package com.feple.feple_backend.notification.repository;

import com.feple.feple_backend.notification.entity.BroadcastNotification;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BroadcastNotificationRepository extends JpaRepository<BroadcastNotification, Long> {
    List<BroadcastNotification> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 가입 이전에 발송된 전체 공지는 신규 유저에게 노출되면 안 되므로 가입일 이후 것만 조회한다.
    List<BroadcastNotification> findByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            LocalDateTime since, Pageable pageable);
}
