package com.feple.feple_backend.admin.artist;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ArtistListParamsTest {

    @Test
    void page_keyword_sort_null이면_기본값_적용() {
        ArtistListParams params = new ArtistListParams(null, null, null);

        assertThat(params.page()).isZero();
        assertThat(params.keyword()).isEmpty();
        assertThat(params.sort()).isEmpty();
    }

    @Test
    void toRedirectUrl_키워드_정렬_없으면_page만_포함() {
        ArtistListParams params = new ArtistListParams(0, null, null);

        assertThat(params.toRedirectUrl()).isEqualTo("/admin/artists?page=0");
    }

    @Test
    void toRedirectUrl_키워드_있으면_포함() {
        ArtistListParams params = new ArtistListParams(0, "iu", null);

        assertThat(params.toRedirectUrl()).isEqualTo("/admin/artists?page=0&keyword=iu");
    }

    @Test
    void toRedirectUrl_정렬_있으면_포함() {
        ArtistListParams params = new ArtistListParams(1, null, "name");

        assertThat(params.toRedirectUrl()).isEqualTo("/admin/artists?page=1&sort=name");
    }

    @Test
    void toRedirectUrl_키워드와_정렬_모두_있으면_모두_포함() {
        ArtistListParams params = new ArtistListParams(2, "아이유", "name");

        assertThat(params.toRedirectUrl()).contains("page=2").contains("sort=name");
    }
}
