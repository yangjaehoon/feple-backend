package com.feple.feple_backend.admin;

import org.springframework.stereotype.Component;

@Component
class InterparkScraperStrategy implements SiteScraperStrategy {

    @Override public String source() { return "interpark"; }

    @Override public String[] titleSelectors() {
        return new String[]{".GoodsDetail .title", ".box_concert_name span", "h3.tit_goods", ".goods_name"};
    }

    @Override public String[] descriptionSelectors() {
        return new String[]{".goods_detail_info", ".box_con_detail .con"};
    }

    @Override public String[] locationHeaders() {
        return new String[]{"장소", "공연장소", "행사장소"};
    }

    @Override public String[] dateHeaders() {
        return new String[]{"기간", "공연기간", "행사기간"};
    }
}
