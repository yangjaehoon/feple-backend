package com.feple.feple_backend.admin.scraper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class FestivalDateParserTest {

    // ── parseRange ──────────────────────────────────────────────────────────

    @ParameterizedTest(name = "구분자 \"{0}\" → {1}")
    @CsvSource({
        "2025.06.22, 2025-06-22",
        "2025-06-22, 2025-06-22",
        "2025/06/22, 2025-06-22",
    })
    void 구분자_종류_무관하게_ISO_날짜로_파싱(String input, String expected) {
        assertThat(FestivalDateParser.parseRange(input)[0]).isEqualTo(expected);
    }

    @Test
    void 한글_구분자_날짜_파싱() {
        String[] result = FestivalDateParser.parseRange("2025년 6월 22일");
        assertThat(result[0]).isEqualTo("2025-06-22");
        assertThat(result[1]).isEqualTo("2025-06-22");
    }

    @Test
    void 단일날짜는_시작과_종료가_같음() {
        String[] result = FestivalDateParser.parseRange("2025.06.22");
        assertThat(result[0]).isEqualTo("2025-06-22");
        assertThat(result[1]).isEqualTo("2025-06-22");
    }

    @Test
    void 날짜_범위_시작과_종료_각각_파싱() {
        String[] result = FestivalDateParser.parseRange("2025.06.22 ~ 2025.06.24");
        assertThat(result[0]).isEqualTo("2025-06-22");
        assertThat(result[1]).isEqualTo("2025-06-24");
    }

    @Test
    void 날짜_범위_월이_다른_경우() {
        String[] result = FestivalDateParser.parseRange("2025.06.28 ~ 2025.07.01");
        assertThat(result[0]).isEqualTo("2025-06-28");
        assertThat(result[1]).isEqualTo("2025-07-01");
    }

    @Test
    void 한자리_월_일도_올바르게_파싱() {
        assertThat(FestivalDateParser.parseRange("2025.6.3")[0]).isEqualTo("2025-06-03");
    }

    @Test
    void 날짜_패턴_없으면_빈_배열_반환() {
        String[] result = FestivalDateParser.parseRange("날짜없음");
        assertThat(result[0]).isEqualTo("");
        assertThat(result[1]).isEqualTo("");
    }

    @Test
    void 빈_문자열이면_빈_배열_반환() {
        String[] result = FestivalDateParser.parseRange("");
        assertThat(result[0]).isEqualTo("");
        assertThat(result[1]).isEqualTo("");
    }

    @Test
    void 존재하지_않는_월은_빈_배열_반환() {
        // DateTimeException 발생 → format()이 "" 반환
        String[] result = FestivalDateParser.parseRange("2025.13.01");
        assertThat(result[0]).isEqualTo("");
        assertThat(result[1]).isEqualTo("");
    }

    @Test
    void 존재하지_않는_일은_빈_배열_반환() {
        String[] result = FestivalDateParser.parseRange("2025.02.30");
        assertThat(result[0]).isEqualTo("");
        assertThat(result[1]).isEqualTo("");
    }

    // ── normalize ───────────────────────────────────────────────────────────

    @Test
    void null_입력시_빈_문자열_반환() {
        assertThat(FestivalDateParser.normalize(null)).isEqualTo("");
    }

    @Test
    void 빈_문자열_입력시_빈_문자열_반환() {
        assertThat(FestivalDateParser.normalize("")).isEqualTo("");
    }

    @Test
    void 공백만_있으면_빈_문자열_반환() {
        assertThat(FestivalDateParser.normalize("   ")).isEqualTo("");
    }

    @Test
    void ISO_날짜_그대로_반환() {
        assertThat(FestivalDateParser.normalize("2025-06-22")).isEqualTo("2025-06-22");
    }

    @Test
    void ISO_날짜_뒤_시각_부분_제거() {
        assertThat(FestivalDateParser.normalize("2025-06-22T10:30:00")).isEqualTo("2025-06-22");
    }

    @Test
    void 점_구분자를_ISO_형식으로_정규화() {
        assertThat(FestivalDateParser.normalize("2025.06.22")).isEqualTo("2025-06-22");
    }

    @Test
    void 날짜_패턴_없으면_빈_문자열_반환() {
        assertThat(FestivalDateParser.normalize("텍스트만")).isEqualTo("");
    }

    // ── extractJsonValue ────────────────────────────────────────────────────

    @Test
    void JSON에서_지정된_키의_값_추출() {
        String json = "{\"title\": \"Feple Festival\", \"date\": \"2025.06.22\"}";
        assertThat(FestivalDateParser.extractJsonValue(json, "title")).isEqualTo("Feple Festival");
    }

    @Test
    void JSON에서_여러_키_중_원하는_키만_추출() {
        String json = "{\"title\": \"Feple Festival\", \"date\": \"2025.06.22\"}";
        assertThat(FestivalDateParser.extractJsonValue(json, "date")).isEqualTo("2025.06.22");
    }

    @Test
    void 존재하지_않는_키는_빈_문자열_반환() {
        String json = "{\"title\": \"Feple Festival\"}";
        assertThat(FestivalDateParser.extractJsonValue(json, "location")).isEqualTo("");
    }

    @Test
    void 빈_JSON이면_빈_문자열_반환() {
        assertThat(FestivalDateParser.extractJsonValue("{}", "key")).isEqualTo("");
    }
}
