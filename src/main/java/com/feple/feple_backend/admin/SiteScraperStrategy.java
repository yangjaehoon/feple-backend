package com.feple.feple_backend.admin;

interface SiteScraperStrategy {
    String source();
    String[] titleSelectors();
    String[] descriptionSelectors();
    String[] locationHeaders();
    String[] dateHeaders();
}
