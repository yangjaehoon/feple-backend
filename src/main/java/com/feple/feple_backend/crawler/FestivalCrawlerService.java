package com.feple.feple_backend.crawler;

import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.entity.FestivalStatus;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FestivalCrawlerService {

    private final FestivalRepository festivalRepository;
    private final List<FestivalSiteCrawler> crawlers; // 모든 구현체 자동 주입

    @Transactional
    public int crawlAll() {
        int totalSaved = 0;
        for (FestivalSiteCrawler crawler : crawlers) {
            log.info("[Crawler] {} 시작", crawler.getSiteName());
            List<CrawledFestivalData> items = crawler.crawl();
            int saved = 0;
            for (CrawledFestivalData data : items) {
                // 동일 URL이 이미 DB에 있으면 스킵
                if (data.getSourceUrl() != null &&
                        festivalRepository.existsBySourceUrl(data.getSourceUrl())) {
                    continue;
                }
                Festival festival = Festival.builder()
                        .title(data.getTitle())
                        .description(data.getDescription() != null ? data.getDescription() : "")
                        .location(data.getLocation() != null ? data.getLocation() : "")
                        .startDate(data.getStartDate())
                        .endDate(data.getEndDate())
                        .posterKey(data.getPosterImageUrl()) // 외부 URL 임시 저장
                        .status(FestivalStatus.DRAFT)
                        .sourceUrl(data.getSourceUrl())
                        .sourceSite(data.getSourceSite())
                        .build();
                festivalRepository.save(festival);
                saved++;
            }
            log.info("[Crawler] {} → {}개 신규 저장", crawler.getSiteName(), saved);
            totalSaved += saved;
        }
        return totalSaved;
    }
}
