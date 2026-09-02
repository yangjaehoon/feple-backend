package com.feple.feple_backend.notification.scheduler;

import com.feple.feple_backend.global.KoreaClock;
import com.feple.feple_backend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PendingPushScheduler {

    private final NotificationService notificationService;

    /** 매일 오전 9시(KST) 실행 — 00:00~09:00 사이 쌓인 자동 알림 대기열을 발송 */
    @Scheduled(cron = "0 0 9 * * *", zone = KoreaClock.ZONE_ID)
    @SchedulerLock(name = "pendingPushScheduler", lockAtMostFor = "5m", lockAtLeastFor = "1m")
    public void flush() {
        notificationService.flushPendingPushes();
    }
}
