package com.feple.feple_backend.user.scheduler;

import com.feple.feple_backend.global.BatchDeletion;
import com.feple.feple_backend.user.repository.UserAccessLogRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserAccessLogCleanupScheduler {

    private static final int RETENTION_DAYS = 90;

    private final UserAccessLogRepository userAccessLogRepository;

    /** 매일 새벽 3시에 90일 이전 접속 로그 삭제 — 커넥션을 오래 붙잡지 않도록 배치로 나눠 커밋 */
    @Scheduled(cron = "0 0 3 * * *")
    @SchedulerLock(name = "userAccessLogCleanupScheduler", lockAtMostFor = "10m", lockAtLeastFor = "1m")
    public void cleanup() {
        LocalDate cutoff = LocalDate.now().minusDays(RETENTION_DAYS);
        int totalDeleted = BatchDeletion.repeatUntilExhausted(
                () -> userAccessLogRepository.deleteByAccessDateBeforeBatch(cutoff, BatchDeletion.BATCH_SIZE));
        log.info("[UserAccessLogCleanup] {}일 이전 접속 로그 {}건 삭제 완료 (cutoff={})", RETENTION_DAYS, totalDeleted, cutoff);
    }
}
