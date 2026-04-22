package com.feple.feple_backend.crawler;

import java.util.List;

public interface FestivalSiteCrawler {
    List<CrawledFestivalData> crawl();
    String getSiteName();
}
