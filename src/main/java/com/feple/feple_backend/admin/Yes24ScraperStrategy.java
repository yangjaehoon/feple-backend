package com.feple.feple_backend.admin;

import org.springframework.stereotype.Component;

@Component
class Yes24ScraperStrategy implements SiteScraperStrategy {

    @Override public String source() { return "yes24"; }

    @Override public String[] titleSelectors() {
        return new String[]{".goods_name h2", ".tit_goods", "h2.info_title"};
    }

    @Override public String[] descriptionSelectors() {
        return new String[]{".goods_intro"};
    }

    @Override public String[] locationHeaders() {
        return new String[]{"공연장소", "장소", "공연장"};
    }

    @Override public String[] dateHeaders() {
        return new String[]{"공연기간", "기간", "행사기간"};
    }
}
