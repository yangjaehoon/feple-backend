package com.feple.feple_backend.admin.scraper;

import java.util.Map;

/** source 문자열별 스크래핑 설정 레지스트리. 새 사이트를 추가하려면 BY_SOURCE에 항목을 더한다. */
final class SiteScraperConfigs {

    private SiteScraperConfigs() {}

    private static final SiteScraperConfig DEFAULT = new SiteScraperConfig(
            new String[0],
            new String[0],
            new String[]{"장소", "공연장소", "공연장", "행사장소"},
            new String[]{"기간", "공연기간", "행사기간"});

    private static final Map<String, SiteScraperConfig> BY_SOURCE = Map.of(
            "interpark", new SiteScraperConfig(
                    new String[]{".GoodsDetail .title", ".box_concert_name span", "h3.tit_goods", ".goods_name"},
                    new String[]{".goods_detail_info", ".box_con_detail .con"},
                    new String[]{"장소", "공연장소", "행사장소"},
                    new String[]{"기간", "공연기간", "행사기간"}),
            "melon", new SiteScraperConfig(
                    new String[]{".subject_wrap .subject", ".info_tit", ".concert_tit"},
                    new String[0],
                    new String[]{"장소", "공연장소"},
                    new String[]{"기간", "공연기간"}),
            "yes24", new SiteScraperConfig(
                    new String[]{".goods_name h2", ".tit_goods", "h2.info_title"},
                    new String[]{".goods_intro"},
                    new String[]{"공연장소", "장소", "공연장"},
                    new String[]{"공연기간", "기간", "행사기간"})
    );

    static SiteScraperConfig forSource(String source) {
        return BY_SOURCE.getOrDefault(source, DEFAULT);
    }
}
