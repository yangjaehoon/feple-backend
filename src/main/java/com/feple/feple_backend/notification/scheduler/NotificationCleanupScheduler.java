package com.feple.feple_backend.notification.scheduler;

import com.feple.feple_backend.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupScheduler {

    private static final int RETENTION_DAYS = 90;

    private final NotificationRepository notificationRepository;

    /** 매일 새벽 4시에 90일 이전 알림 삭제 */
    @Scheduled(cron = "0 0 4 * * *")
    @SchedulerLock(name = "notificationCleanupScheduler", lockAtMostFor = "10m", lockAtLeastFor = "1m")
    @Transactional
    public void cleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        notificationRepository.deleteOlderThan(cutoff);
        log.info("[NotificationCleanup] {}일 이전 알림 삭제 완료 (cutoff={})", RETENTION_DAYS, cutoff);
    }
}
