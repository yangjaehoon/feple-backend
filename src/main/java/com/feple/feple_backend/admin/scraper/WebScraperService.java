package com.feple.feple_backend.admin.scraper;

import com.feple.feple_backend.admin.ocr.GeminiUrlContextClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebScraperService {

    private final FestivalPageScraper festivalPageScraper;
    private final GeminiUrlContextClient geminiUrlContextClient;

    // Jsoup을 먼저 시도한다 — 빠르고 무료. 인터파크·멜론 같은 SPA는 JS 렌더링이 필요해
    // Jsoup으로 콘텐츠를 가져오지 못하므로, 그때만 Gemini URL context API(유료)로 폴백한다.
    public ScrapedFestivalDto scrape(String url, String source) throws IOException {
        ScrapedFestivalDto jsoupResult = festivalPageScraper.scrape(url, source);

        if (festivalPageScraper.isSpaOrEmpty(jsoupResult) && geminiUrlContextClient.isConfigured()) {
            log.info("SPA detected, falling back to Gemini URL context for: {}", url);
            try {
                return geminiUrlContextClient.scrape(url, source);
            } catch (Exception e) {
                log.warn("Gemini URL context failed for {}: {}", url, e.getMessage());
            }
        }

        return jsoupResult;
    }
}
