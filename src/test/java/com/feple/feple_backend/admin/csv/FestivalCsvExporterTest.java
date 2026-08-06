package com.feple.feple_backend.admin.csv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.entity.Region;
import com.feple.feple_backend.festival.service.FestivalAdminService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FestivalCsvExporterTest {

    @Mock FestivalAdminService festivalAdminService;

    @InjectMocks FestivalCsvExporter exporter;

    @Test
    void buildCsv_페스티벌없으면_헤더만_반환() {
        given(festivalAdminService.getAllFestivalsForAdmin()).willReturn(List.of());

        assertThat(exporter.buildCsv()).isEqualTo("ID,제목,영어제목,지역,장소,시작일,종료일,좋아요,참석의사\n");
    }

    @Test
    void buildCsv_페스티벌_정보를_행으로_변환() {
        FestivalResponseDto festival = FestivalResponseDto.builder()
                .id(1L).title("록페스티벌").titleEn("Rock Fest").region(Region.SEOUL)
                .location("서울숲").startDate(LocalDate.of(2026, 8, 1)).endDate(LocalDate.of(2026, 8, 3))
                .likeCount(5).attendingCount(20).build();
        given(festivalAdminService.getAllFestivalsForAdmin()).willReturn(List.of(festival));

        assertThat(exporter.buildCsv())
                .contains("1,록페스티벌,Rock Fest,서울,서울숲,2026-08-01,2026-08-03,5,20\n");
    }

    @Test
    void buildCsv_지역없으면_빈값() {
        FestivalResponseDto festival = FestivalResponseDto.builder()
                .id(1L).title("록페스티벌").build();
        given(festivalAdminService.getAllFestivalsForAdmin()).willReturn(List.of(festival));

        assertThat(exporter.buildCsv()).contains("1,록페스티벌,,,,,,0,0\n");
    }
}
