package com.feple.feple_backend.admin.scraper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SiteScraperConfigsTest {

    @Test
    void 등록된_소스는_전용_셀렉터를_반환한다() {
        SiteScraperConfig config = SiteScraperConfigs.forSource("interpark");

        assertThat(config.titleSelectors()).contains(".GoodsDetail .title");
        assertThat(config.locationHeaders()).contains("장소", "공연장소", "행사장소");
    }

    @Test
    void 등록되지_않은_소스는_기본_설정을_반환한다() {
        SiteScraperConfig config = SiteScraperConfigs.forSource("unknown-site");

        assertThat(config.titleSelectors()).isEmpty();
        assertThat(config.descriptionSelectors()).isEmpty();
        assertThat(config.locationHeaders()).contains("장소", "공연장소", "공연장", "행사장소");
        assertThat(config.dateHeaders()).contains("기간", "공연기간", "행사기간");
    }
}
