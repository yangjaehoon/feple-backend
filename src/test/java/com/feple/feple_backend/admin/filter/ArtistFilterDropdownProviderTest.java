package com.feple.feple_backend.admin.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.service.ArtistAdminService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

@ExtendWith(MockitoExtension.class)
class ArtistFilterDropdownProviderTest {

    @Mock ArtistAdminService artistService;

    @InjectMocks ArtistFilterDropdownProvider provider;

    @Test
    void filterKey는_ARTIST를_반환한다() {
        assertThat(provider.filterKey()).isEqualTo("ARTIST");
    }

    @Test
    void populate시_이름순_아티스트_목록을_모델에_담는다() {
        List<ArtistResponseDto> artists = List.of(ArtistResponseDto.builder().id(1L).name("가수").build());
        given(artistService.getAllArtistsSortedByName()).willReturn(artists);
        Model model = new ExtendedModelMap();

        provider.populate(model);

        assertThat(model.getAttribute("artists")).isEqualTo(artists);
        verify(artistService).getAllArtistsSortedByName();
    }
}
