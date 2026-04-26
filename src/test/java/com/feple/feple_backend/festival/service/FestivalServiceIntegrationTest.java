package com.feple.feple_backend.festival.service;

import com.feple.feple_backend.festival.dto.FestivalRequestDto;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.entity.Genre;
import com.feple.feple_backend.festival.entity.Region;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@ActiveProfiles("test")
@Transactional  // 각 테스트 후 DB 롤백
class FestivalServiceIntegrationTest {

    @Autowired FestivalService festivalService;
    @Autowired FestivalRepository festivalRepository;

    // S3 호출을 가짜로 교체 (실제 AWS 연결 없이)
    @MockBean FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        given(fileStorageService.buildUrl(anyString())).willAnswer(inv ->
                "https://cdn.example.com/" + inv.getArgument(0));
        given(fileStorageService.buildUrl(null)).willReturn(null);
    }

    // ── createFestival ────────────────────────────────────────────

    @Test
    void 페스티벌_생성_후_조회() {
        FestivalRequestDto dto = makeDto("서울재즈페스티벌",
                LocalDate.of(2025, 5, 23), LocalDate.of(2025, 5, 25));

        Long id = festivalService.createFestival(dto);

        FestivalResponseDto result = festivalService.getFestival(id);
        assertThat(result.getTitle()).isEqualTo("서울재즈페스티벌");
        assertThat(result.getLocation()).isEqualTo("올림픽공원");
        assertThat(result.getRegion()).isEqualTo(Region.SEOUL);
    }

    @Test
    void 생성된_페스티벌은_DB에_저장됨() {
        long before = festivalRepository.count();

        festivalService.createFestival(makeDto("테스트페스티벌",
                LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 3)));

        assertThat(festivalRepository.count()).isEqualTo(before + 1);
    }

    // ── getFestival ───────────────────────────────────────────────

    @Test
    void 없는_페스티벌_조회시_404_예외() {
        assertThatThrownBy(() -> festivalService.getFestival(9999L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("9999");
    }

    // ── updateFestival ────────────────────────────────────────────

    @Test
    void 페스티벌_제목_수정() {
        Long id = festivalService.createFestival(makeDto("원래제목",
                LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 3)));

        FestivalRequestDto updateDto = makeDto("수정된제목",
                LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 3));
        festivalService.updateFestival(id, updateDto);

        FestivalResponseDto result = festivalService.getFestival(id);
        assertThat(result.getTitle()).isEqualTo("수정된제목");
    }

    @Test
    void 없는_페스티벌_수정시_404_예외() {
        FestivalRequestDto dto = makeDto("제목", LocalDate.now(), LocalDate.now().plusDays(1));

        assertThatThrownBy(() -> festivalService.updateFestival(9999L, dto))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── deleteFestival ────────────────────────────────────────────

    @Test
    void 페스티벌_삭제_후_조회_불가() {
        Long id = festivalService.createFestival(makeDto("삭제예정",
                LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 2)));

        festivalService.deleteFestival(id);

        assertThatThrownBy(() -> festivalService.getFestival(id))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── getAllFestivals ────────────────────────────────────────────

    @Test
    void 진행중_페스티벌만_필터링() {
        LocalDate future = LocalDate.now().plusDays(10);
        LocalDate past = LocalDate.now().minusDays(5);

        festivalService.createFestival(makeDto("진행중", future, future.plusDays(3)));
        festivalService.createFestival(makeDtoEnded("종료됨", past.minusDays(3), past));

        List<FestivalResponseDto> activeOnly =
                festivalService.getAllFestivals(null, null, false);
        List<FestivalResponseDto> all =
                festivalService.getAllFestivals(null, null, true);

        long activeCount = activeOnly.stream()
                .filter(f -> f.getTitle().equals("진행중")).count();
        long endedCount = activeOnly.stream()
                .filter(f -> f.getTitle().equals("종료됨")).count();

        assertThat(activeCount).isEqualTo(1);
        assertThat(endedCount).isEqualTo(0);
        assertThat(all.size()).isGreaterThan(activeOnly.size());
    }

    @Test
    void 장르_필터링() {
        festivalService.createFestival(makeDtoWithGenre("밴드페스티벌", Genre.BAND));
        festivalService.createFestival(makeDtoWithGenre("힙합페스티벌", Genre.HIP_HOP));

        List<FestivalResponseDto> result =
                festivalService.getAllFestivals(List.of(Genre.BAND), null, true);

        assertThat(result).allMatch(f -> f.getGenres().contains(Genre.BAND));
    }

    // ── 헬퍼 메서드 ───────────────────────────────────────────────

    private FestivalRequestDto makeDto(String title, LocalDate start, LocalDate end) {
        FestivalRequestDto dto = new FestivalRequestDto();
        dto.setTitle(title);
        dto.setDescription("설명");
        dto.setLocation("올림픽공원");
        dto.setStartDate(start);
        dto.setEndDate(end);
        dto.setRegion(Region.SEOUL);
        dto.setGenres(List.of(Genre.BAND));
        return dto;
    }

    private FestivalRequestDto makeDtoEnded(String title, LocalDate start, LocalDate end) {
        // 이미 종료된 페스티벌 (endDate가 과거)
        return makeDto(title, start, end);
    }

    private FestivalRequestDto makeDtoWithGenre(String title, Genre genre) {
        FestivalRequestDto dto = makeDto(title,
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(7));
        dto.setGenres(List.of(genre));
        return dto;
    }
}
