package com.feple.feple_backend.admin.csv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.service.ArtistAdminService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArtistCsvExporterTest {

    @Mock ArtistAdminService artistAdminService;

    @InjectMocks ArtistCsvExporter exporter;

    @Test
    void buildCsv_아티스트없으면_헤더만_반환() {
        given(artistAdminService.getArtistsForExport()).willReturn(List.of());

        assertThat(exporter.buildCsv()).isEqualTo("ID,이름,영어이름,카테고리,팔로워수,곡수\n");
    }

    @Test
    void buildCsv_아티스트_정보를_행으로_변환() {
        ArtistResponseDto artist = ArtistResponseDto.builder()
                .id(1L).name("아이유").nameEn("IU").genre("발라드")
                .followerCount(10).songCount(3).build();
        given(artistAdminService.getArtistsForExport()).willReturn(List.of(artist));

        assertThat(exporter.buildCsv()).contains("1,아이유,IU,발라드,10,3\n");
    }
}
