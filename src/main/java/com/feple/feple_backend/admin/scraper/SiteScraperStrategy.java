package com.feple.feple_backend.admin.scraper;

interface SiteScraperStrategy {
    String source();
    String[] titleSelectors();
    String[] descriptionSelectors();
    String[] locationHeaders();
    String[] dateHeaders();
}
