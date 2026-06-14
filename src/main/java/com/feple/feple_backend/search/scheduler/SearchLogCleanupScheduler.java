package com.feple.feple_backend.search.scheduler;

import com.feple.feple_backend.search.repository.SearchLogRepository;
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
public class SearchLogCleanupScheduler {

    private static final int RETENTION_DAYS = 90;

    private final SearchLogRepository searchLogRepository;

    /** 매일 새벽 3시에 90일 이전 검색 로그 삭제 */
    @Scheduled(cron = "0 0 3 * * *")
    @SchedulerLock(name = "searchLogCleanupScheduler", lockAtMostFor = "10m", lockAtLeastFor = "1m")
    @Transactional
    public void cleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        searchLogRepository.deleteByCreatedAtBefore(cutoff);
        log.info("[SearchLogCleanup] {}일 이전 검색 로그 삭제 완료 (cutoff={})", RETENTION_DAYS, cutoff);
    }
}
