package com.feple.feple_backend.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.feple.feple_backend.global.exception.InvalidRequestException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

class AdminUrlUtilsTest {

    // ── buildQueryString ──────────────────────────────────────────────────────

    @Test
    void buildQueryString_key_value_쌍을_선행_물음표_없이_조립() {
        assertThat(AdminUrlUtils.buildQueryString("type", "USER", "page", 2))
                .isEqualTo("type=USER&page=2");
    }

    @Test
    void buildQueryString_null이거나_공백인_값은_생략() {
        assertThat(AdminUrlUtils.buildQueryString("type", null, "keyword", "  ", "page", 0))
                .isEqualTo("page=0");
    }

    @Test
    void buildQueryString_값이_하나도_없으면_빈_문자열() {
        assertThat(AdminUrlUtils.buildQueryString("keyword", null)).isEmpty();
    }

    @Test
    void buildQueryString_key_value_개수가_홀수면_예외() {
        assertThatThrownBy(() -> AdminUrlUtils.buildQueryString("type", "USER", "page"))
                .isInstanceOf(InvalidRequestException.class);
    }

    // ── listUrl ───────────────────────────────────────────────────────────────

    @Test
    void listUrl_basePath에_파라미터를_붙이고_공백값은_생략() {
        assertThat(AdminUrlUtils.listUrl("/admin/festivals", "page", 0, "keyword", ""))
                .isEqualTo("/admin/festivals?page=0");
    }

    @Test
    void listUrl_한글_키워드는_퍼센트_인코딩되어_원문이_노출되지_않는다() {
        String url = AdminUrlUtils.listUrl("/admin/festivals", "page", 0, "keyword", "축제");

        assertThat(url).doesNotContain("축제");
        assertThat(URLDecoder.decode(url, StandardCharsets.UTF_8))
                .isEqualTo("/admin/festivals?page=0&keyword=축제");
    }

    // ── toQueryString / toEncodedString ───────────────────────────────────────

    @Test
    void toQueryString_선행_물음표를_제거() {
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance().queryParam("a", "1");
        assertThat(AdminUrlUtils.toQueryString(builder)).isEqualTo("a=1");
    }

    @Test
    void toEncodedString_한글_값을_인코딩() {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/x").queryParam("q", "한글");
        assertThat(AdminUrlUtils.toEncodedString(builder)).doesNotContain("한글");
    }

    // ── appendIfHasText ───────────────────────────────────────────────────────

    @Test
    void appendIfHasText_텍스트가_있으면_추가() {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/x");
        AdminUrlUtils.appendIfHasText(builder, "keyword", "aespa");
        assertThat(builder.build().toUriString()).isEqualTo("/x?keyword=aespa");
    }

    @Test
    void appendIfHasText_null이나_공백이면_추가하지_않음() {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/x");
        AdminUrlUtils.appendIfHasText(builder, "keyword", null);
        AdminUrlUtils.appendIfHasText(builder, "keyword", "   ");
        assertThat(builder.build().toUriString()).isEqualTo("/x");
    }
}
