package com.feple.feple_backend.admin.artist;

import static org.assertj.core.api.Assertions.assertThat;

import com.feple.feple_backend.global.MusicGenre;
import org.junit.jupiter.api.Test;

class ArtistListParamsTest {

    @Test
    void page_keyword_sort_null이면_기본값_적용() {
        ArtistListParams params = new ArtistListParams(null, null, null, null);

        assertThat(params.page()).isZero();
        assertThat(params.keyword()).isEmpty();
        assertThat(params.sort()).isEmpty();
        assertThat(params.genre()).isNull();
    }

    @Test
    void page_음수면_0으로_정규화() {
        assertThat(new ArtistListParams(-3, null, null, null).page()).isZero();
    }

    @Test
    void toListUrl_키워드_정렬_장르_없으면_page만_포함() {
        ArtistListParams params = new ArtistListParams(0, null, null, null);

        assertThat(params.toListUrl()).isEqualTo("/admin/artists?page=0");
    }

    @Test
    void toListUrl_키워드_있으면_포함() {
        ArtistListParams params = new ArtistListParams(0, "iu", null, null);

        assertThat(params.toListUrl()).isEqualTo("/admin/artists?page=0&keyword=iu");
    }

    @Test
    void toListUrl_정렬_있으면_포함() {
        ArtistListParams params = new ArtistListParams(1, null, "name", null);

        assertThat(params.toListUrl()).isEqualTo("/admin/artists?page=1&sort=name");
    }

    @Test
    void toListUrl_장르_있으면_포함() {
        ArtistListParams params = new ArtistListParams(0, null, null, MusicGenre.INDIE);

        assertThat(params.toListUrl()).isEqualTo("/admin/artists?page=0&genre=INDIE");
    }

    @Test
    void toListUrl_키워드와_정렬_모두_있으면_모두_포함() {
        ArtistListParams params = new ArtistListParams(2, "아이유", "name", null);

        assertThat(params.toListUrl()).contains("page=2").contains("sort=name");
    }
}
