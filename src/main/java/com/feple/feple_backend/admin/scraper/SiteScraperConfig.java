package com.feple.feple_backend.admin.scraper;

/** 사이트별 Jsoup CSS 셀렉터·테이블 헤더 텍스트 모음 — 순수 설정 데이터라 사이트마다 클래스를 두지 않는다. */
record SiteScraperConfig(String[] titleSelectors,
                          String[] descriptionSelectors,
                          String[] locationHeaders,
                          String[] dateHeaders) {
}
