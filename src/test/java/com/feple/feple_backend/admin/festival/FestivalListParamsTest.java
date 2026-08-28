package com.feple.feple_backend.admin.festival;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FestivalListParamsTest {

    @Test
    void null_음수는_정규화된다() {
        FestivalListParams params = new FestivalListParams(-2, null);

        assertThat(params.page()).isZero();
        assertThat(params.keyword()).isEmpty();
    }

    @Test
    void toListUrl_page만_있으면_page만_포함() {
        assertThat(new FestivalListParams(0, "  ").toListUrl())
                .isEqualTo("/admin/festivals?page=0");
    }

    @Test
    void toListUrl_한글_키워드는_인코딩된다() {
        String url = new FestivalListParams(1, "펜타포트").toListUrl();

        assertThat(url).startsWith("/admin/festivals?page=1&keyword=").doesNotContain("펜타포트");
    }
}
