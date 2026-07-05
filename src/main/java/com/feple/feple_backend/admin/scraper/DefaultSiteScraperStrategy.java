package com.feple.feple_backend.admin.scraper;

class DefaultSiteScraperStrategy implements SiteScraperStrategy {

    static final DefaultSiteScraperStrategy INSTANCE = new DefaultSiteScraperStrategy();

    private DefaultSiteScraperStrategy() {}

    // Spring 빈이 아니라 FestivalPageScraper가 getOrDefault()로 직접 꺼내 쓰는
    // 폴백 인스턴스라 source()는 맵 키로 조회되지 않는다 — 인터페이스 계약 충족용.
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
