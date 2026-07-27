package com.feple.feple_backend.admin.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.service.FestivalAdminService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

@ExtendWith(MockitoExtension.class)
class FestivalFilterDropdownProviderTest {

    @Mock FestivalAdminService festivalService;

    @InjectMocks FestivalFilterDropdownProvider provider;

    @Test
    void filterKey는_FESTIVAL을_반환한다() {
        assertThat(provider.filterKey()).isEqualTo("FESTIVAL");
    }

    @Test
    void populate시_전체_페스티벌_목록을_모델에_담는다() {
        List<FestivalResponseDto> festivals = List.of(FestivalResponseDto.builder().id(1L).title("록페").build());
        given(festivalService.getAllFestivalsForAdmin()).willReturn(festivals);
        Model model = new ExtendedModelMap();

        provider.populate(model);

        assertThat(model.getAttribute("festivals")).isEqualTo(festivals);
        verify(festivalService).getAllFestivalsForAdmin();
    }
}
