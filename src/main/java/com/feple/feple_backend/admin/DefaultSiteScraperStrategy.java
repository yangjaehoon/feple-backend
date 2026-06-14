package com.feple.feple_backend.admin;

class DefaultSiteScraperStrategy implements SiteScraperStrategy {

    static final DefaultSiteScraperStrategy INSTANCE = new DefaultSiteScraperStrategy();

    private DefaultSiteScraperStrategy() {}

    @Override public String source() { return ""; }
    @Override public String[] titleSelectors() { return new String[0]; }
    @Override public String[] descriptionSelectors() { return new String[0]; }

    @Override public String[] locationHeaders() {
        return new String[]{"장소", "공연장소", "공연장", "행사장소"};
    }

    @Override public String[] dateHeaders() {
        return new String[]{"기간", "공연기간", "행사기간"};
    }
}
