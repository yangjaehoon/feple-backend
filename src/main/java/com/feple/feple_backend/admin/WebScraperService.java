package com.feple.feple_backend.admin;

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
