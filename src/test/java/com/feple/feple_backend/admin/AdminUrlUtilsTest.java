package com.feple.feple_backend.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void buildQueryString_key_value_개수가_홀수면_호출부_실수로_IllegalStateException() {
        assertThatThrownBy(() -> AdminUrlUtils.buildQueryString("type", "USER", "page"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void buildQueryString_key가_String이_아니면_호출부_실수로_IllegalStateException() {
        assertThatThrownBy(() -> AdminUrlUtils.buildQueryString(42, "USER"))
                .isInstanceOf(IllegalStateException.class);
    }

    // ── listUrl ───────────────────────────────────────────────────────────────

    @Test
    void listUrl_basePath에_파라미터를_붙이고_null이나_공백값은_생략() {
        assertThat(AdminUrlUtils.listUrl("/admin/festivals", "page", 0, "sort", null, "keyword", ""))
                .isEqualTo("/admin/festivals?page=0");
    }

    @Test
    void listUrl도_홀수_개수나_비String_key면_IllegalStateException() {
        assertThatThrownBy(() -> AdminUrlUtils.listUrl("/admin/x", "page"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> AdminUrlUtils.listUrl("/admin/x", 1, "v"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void listUrl_한글_키워드는_퍼센트_인코딩되어_원문이_노출되지_않는다() {
        String url = AdminUrlUtils.listUrl("/admin/festivals", "page", 0, "keyword", "축제");

        assertThat(url).doesNotContain("축제");
        assertThat(URLDecoder.decode(url, StandardCharsets.UTF_8))
                .isEqualTo("/admin/festivals?page=0&keyword=축제");
    }

    // ── encode / encodeQuery ──────────────────────────────────────────────────

    @Test
    void encodeQuery_선행_물음표를_제거() {
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance().queryParam("a", "1");
        assertThat(AdminUrlUtils.encodeQuery(builder)).isEqualTo("a=1");
    }

    @Test
    void encode_한글_값을_퍼센트_인코딩한다() {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/x").queryParam("q", "한글");
        String encoded = AdminUrlUtils.encode(builder);

        assertThat(encoded).doesNotContain("한글");
        assertThat(URLDecoder.decode(encoded, StandardCharsets.UTF_8)).isEqualTo("/x?q=한글");
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
