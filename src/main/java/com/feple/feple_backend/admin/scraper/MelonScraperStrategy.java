package com.feple.feple_backend.admin.scraper;

import org.springframework.stereotype.Component;

@Component
class MelonScraperStrategy implements SiteScraperStrategy {

    @Override public String source() { return "melon"; }

    @Override public String[] titleSelectors() {
        return new String[]{".subject_wrap .subject", ".info_tit", ".concert_tit"};
    }

    @Override public String[] descriptionSelectors() {
        return new String[0];
    }

    @Override public String[] locationHeaders() {
        return new String[]{"장소", "공연장소"};
    }

    @Override public String[] dateHeaders() {
        return new String[]{"기간", "공연기간"};
    }
}
