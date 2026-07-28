package com.feple.feple_backend.admin.post;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PostListParamsTest {

    @Test
    void page_null이면_0으로_기본값_적용() {
        PostListParams params = new PostListParams(null, "FREE", null, null, null);

        assertThat(params.page()).isZero();
    }

    @Test
    void filter_keyword_null이면_빈문자열로_기본값_적용() {
        PostListParams params = new PostListParams(0, null, null, null, null);

        assertThat(params.filter()).isEmpty();
        assertThat(params.keyword()).isEmpty();
    }

    @Test
    void toExtraParams_키워드_공백이면_생략() {
        PostListParams params = new PostListParams(0, "FREE", "  ", null, null);

        assertThat(params.toExtraParams()).isEqualTo("filter=FREE");
    }

    @Test
    void toExtraParams_키워드_있으면_포함() {
        PostListParams params = new PostListParams(0, "FREE", "공지", null, null);

        assertThat(params.toExtraParams()).isEqualTo("filter=FREE&keyword=공지");
    }

    @Test
    void toExtraParams_artistId_festivalId_있으면_모두_포함() {
        PostListParams params = new PostListParams(0, "ARTIST", null, 3L, 5L);

        assertThat(params.toExtraParams()).isEqualTo("filter=ARTIST&artistId=3&festivalId=5");
    }

    @Test
    void toRedirectParams_page와_filter만_있으면_기본형태() {
        PostListParams params = new PostListParams(2, "FREE", null, null, null);

        assertThat(params.toRedirectParams()).isEqualTo("filter=FREE&page=2");
    }

    @Test
    void toRedirectParams_artistId_festivalId_있으면_모두_포함() {
        PostListParams params = new PostListParams(1, "ARTIST", null, 3L, 5L);

        assertThat(params.toRedirectParams()).isEqualTo("filter=ARTIST&page=1&artistId=3&festivalId=5");
    }
}
