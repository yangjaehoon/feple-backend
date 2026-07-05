package com.feple.feple_backend.stage;

import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.stage.entity.Stage;
import com.feple.feple_backend.stage.repository.StageRepository;
import com.feple.feple_backend.stage.service.StageService;
import com.feple.feple_backend.timetable.repository.TimetableRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class StageServiceTest {

    @Mock StageRepository stageRepository;
    @Mock FestivalRepository festivalRepository;
    @Mock TimetableRepository timetableRepository;

    @InjectMocks StageService stageService;

    @Test
    void createStage_이름_null_예외() {
        assertThatThrownBy(() -> stageService.createStage(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("스테이지 이름을 입력해주세요.");
    }

    @Test
    void createStage_이름_blank_예외() {
        assertThatThrownBy(() -> stageService.createStage(1L, "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("스테이지 이름을 입력해주세요.");
    }

    @Test
    void createStage_이름_51자_예외() {
        String longName = "a".repeat(51);
        assertThatThrownBy(() -> stageService.createStage(1L, longName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("50자 이하");
    }

    @Test
    void createStage_성공_displayOrder_1로_설정() {
        Festival festival = org.mockito.Mockito.mock(Festival.class);
        given(festivalRepository.findById(1L)).willReturn(Optional.of(festival));
        given(stageRepository.findMaxDisplayOrderByFestivalId(1L)).willReturn(0);

        Stage savedStage = Stage.builder()
                .festival(festival)
                .name("MAIN STAGE")
                .displayOrder(1)
                .build();
        given(stageRepository.save(any(Stage.class))).willReturn(savedStage);

        Stage result = stageService.createStage(1L, "MAIN STAGE");

        ArgumentCaptor<Stage> captor = ArgumentCaptor.forClass(Stage.class);
        then(stageRepository).should().save(captor.capture());
        assertThat(captor.getValue().getDisplayOrder()).isEqualTo(1);
        assertThat(captor.getValue().getName()).isEqualTo("MAIN STAGE");
    }

    @Test
    void deleteStage_nullify_후_삭제_호출() {
        stageService.deleteStage(3L);

        then(timetableRepository).should().nullifyStageId(3L);
        then(stageRepository).should().deleteById(3L);
    }

    @Test
    void moveUp_앞_스테이지와_순서_교환() {
        Festival festival = org.mockito.Mockito.mock(Festival.class);
        given(festival.getId()).willReturn(1L);
        Stage current = Stage.builder().festival(festival).name("CURRENT").displayOrder(3).build();
        Stage prev = Stage.builder().festival(festival).name("PREV").displayOrder(2).build();

        given(stageRepository.findById(10L)).willReturn(Optional.of(current));
        given(stageRepository.findFirstByFestivalIdAndDisplayOrderLessThanOrderByDisplayOrderDesc(1L, 3))
                .willReturn(Optional.of(prev));

        stageService.moveUp(1L, 10L);

        assertThat(current.getDisplayOrder()).isEqualTo(2);
        assertThat(prev.getDisplayOrder()).isEqualTo(3);
    }

    @Test
    void moveUp_앞_스테이지_없으면_교환_없음() {
        Festival festival = org.mockito.Mockito.mock(Festival.class);
        given(festival.getId()).willReturn(1L);
        Stage current = Stage.builder().festival(festival).name("CURRENT").displayOrder(1).build();

        given(stageRepository.findById(10L)).willReturn(Optional.of(current));
        given(stageRepository.findFirstByFestivalIdAndDisplayOrderLessThanOrderByDisplayOrderDesc(1L, 1))
                .willReturn(Optional.empty());

        stageService.moveUp(1L, 10L);

        assertThat(current.getDisplayOrder()).isEqualTo(1);
    }

    @Test
    void moveDown_뒤_스테이지와_순서_교환() {
        Festival festival = org.mockito.Mockito.mock(Festival.class);
        given(festival.getId()).willReturn(1L);
        Stage current = Stage.builder().festival(festival).name("CURRENT").displayOrder(2).build();
        Stage next = Stage.builder().festival(festival).name("NEXT").displayOrder(3).build();

        given(stageRepository.findById(10L)).willReturn(Optional.of(current));
        given(stageRepository.findFirstByFestivalIdAndDisplayOrderGreaterThanOrderByDisplayOrderAsc(1L, 2))
                .willReturn(Optional.of(next));

        stageService.moveDown(1L, 10L);

        assertThat(current.getDisplayOrder()).isEqualTo(3);
        assertThat(next.getDisplayOrder()).isEqualTo(2);
    }

    @Test
    void moveUp_다른_페스티벌_스테이지면_예외() {
        Festival festival = org.mockito.Mockito.mock(Festival.class);
        given(festival.getId()).willReturn(2L);
        Stage current = Stage.builder().festival(festival).name("CURRENT").displayOrder(3).build();

        given(stageRepository.findById(10L)).willReturn(Optional.of(current));

        assertThatThrownBy(() -> stageService.moveUp(1L, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 페스티벌의 스테이지가 아닙니다.");
    }

    @Test
    void moveDown_다른_페스티벌_스테이지면_예외() {
        Festival festival = org.mockito.Mockito.mock(Festival.class);
        given(festival.getId()).willReturn(2L);
        Stage current = Stage.builder().festival(festival).name("CURRENT").displayOrder(3).build();

        given(stageRepository.findById(10L)).willReturn(Optional.of(current));

        assertThatThrownBy(() -> stageService.moveDown(1L, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 페스티벌의 스테이지가 아닙니다.");
    }
}
