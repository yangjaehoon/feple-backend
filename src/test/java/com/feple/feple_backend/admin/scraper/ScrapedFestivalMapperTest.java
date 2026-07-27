package com.feple.feple_backend.admin.scraper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.feple.feple_backend.festival.dto.FestivalRequestDto;
import com.feple.feple_backend.festival.entity.Region;
import com.feple.feple_backend.global.MusicGenre;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScrapedFestivalMapperTest {

    @Test
    void 필수_필드와_공백_트림을_반영해_변환한다() {
        ScraperApplyRequestDto req = new ScraperApplyRequestDto(
                "  록 페스티벌  ", " Rock Fest ", " 설명 ", " 서울숲 ",
                "2026-08-01", "2026-08-03", null, null);

        FestivalRequestDto dto = ScrapedFestivalMapper.toFestivalRequestDto(req);

        assertThat(dto.getTitle()).isEqualTo("록 페스티벌");
        assertThat(dto.getTitleEn()).isEqualTo("Rock Fest");
        assertThat(dto.getDescription()).isEqualTo("설명");
        assertThat(dto.getLocation()).isEqualTo("서울숲");
        assertThat(dto.getStartDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(dto.getEndDate()).isEqualTo(LocalDate.of(2026, 8, 3));
        assertThat(dto.getRegion()).isNull();
        assertThat(dto.getGenres()).isNull();
    }

    @Test
    void titleEn과_description_location이_null이면_빈값또는_null로_처리한다() {
        ScraperApplyRequestDto req = new ScraperApplyRequestDto(
                "제목", null, null, null,
                "2026-08-01", "2026-08-03", null, null);

        FestivalRequestDto dto = ScrapedFestivalMapper.toFestivalRequestDto(req);

        assertThat(dto.getTitleEn()).isNull();
        assertThat(dto.getDescription()).isEqualTo("");
        assertThat(dto.getLocation()).isEqualTo("");
    }

    @Test
    void 유효한_지역값은_변환된다() {
        ScraperApplyRequestDto req = new ScraperApplyRequestDto(
                "제목", null, null, null,
                "2026-08-01", "2026-08-03", "SEOUL", null);

        FestivalRequestDto dto = ScrapedFestivalMapper.toFestivalRequestDto(req);

        assertThat(dto.getRegion()).isEqualTo(Region.SEOUL);
    }

    @Test
    void 유효하지_않은_지역값은_예외를_던진다() {
        ScraperApplyRequestDto req = new ScraperApplyRequestDto(
                "제목", null, null, null,
                "2026-08-01", "2026-08-03", "MARS", null);

        assertThatThrownBy(() -> ScrapedFestivalMapper.toFestivalRequestDto(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MARS");
    }

    @Test
    void 유효한_장르_목록은_공백_항목을_제외하고_변환된다() {
        ScraperApplyRequestDto req = new ScraperApplyRequestDto(
                "제목", null, null, null,
                "2026-08-01", "2026-08-03", null, List.of("BAND", "", "INDIE"));

        FestivalRequestDto dto = ScrapedFestivalMapper.toFestivalRequestDto(req);

        assertThat(dto.getGenres()).containsExactly(MusicGenre.BAND, MusicGenre.INDIE);
    }

    @Test
    void 유효하지_않은_장르값은_예외를_던진다() {
        ScraperApplyRequestDto req = new ScraperApplyRequestDto(
                "제목", null, null, null,
                "2026-08-01", "2026-08-03", null, List.of("트로트"));

        assertThatThrownBy(() -> ScrapedFestivalMapper.toFestivalRequestDto(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("트로트");
    }
}
