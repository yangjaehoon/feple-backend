package com.feple.feple_backend.search.scheduler;

import com.feple.feple_backend.search.repository.SearchLogRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchLogCleanupScheduler {

    private static final int RETENTION_DAYS = 90;
    private static final int BATCH_SIZE = 1000;

    private final SearchLogRepository searchLogRepository;

    /** 매일 새벽 3시에 90일 이전 검색 로그 삭제 — 커넥션을 오래 붙잡지 않도록 배치로 나눠 커밋 */
    @Scheduled(cron = "0 0 3 * * *")
    @SchedulerLock(name = "searchLogCleanupScheduler", lockAtMostFor = "10m", lockAtLeastFor = "1m")
    public void cleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        int totalDeleted = 0;
        int deleted;
        do {
            deleted = searchLogRepository.deleteByCreatedAtBeforeBatch(cutoff, BATCH_SIZE);
            totalDeleted += deleted;
        } while (deleted == BATCH_SIZE);
        log.info("[SearchLogCleanup] {}일 이전 검색 로그 {}건 삭제 완료 (cutoff={})", RETENTION_DAYS, totalDeleted, cutoff);
    }
}
