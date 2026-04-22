package com.feple.feple_backend.crawler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlerScheduler {

    private final FestivalCrawlerService crawlerService;

    // 매일 오전 9시 자동 실행
    @Scheduled(cron = "0 0 9 * * *")
    public void scheduledCrawl() {
        log.info("[CrawlerScheduler] 자동 크롤링 시작");
        int count = crawlerService.crawlAll();
        log.info("[CrawlerScheduler] 자동 크롤링 완료 — 신규 {}개", count);
    }
}
