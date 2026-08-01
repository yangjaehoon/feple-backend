package com.feple.feple_backend.festival.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.festival.dto.FestivalDetailResponseDto;
import com.feple.feple_backend.festival.dto.FestivalFilterCriteria;
import com.feple.feple_backend.festival.dto.FestivalRequestDto;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.entity.FestivalLike;
import com.feple.feple_backend.festival.entity.Region;
import com.feple.feple_backend.festival.repository.FestivalLikeRepository;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.support.TestEntityFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class FestivalServiceImplTest {

    @Mock FestivalRepository festivalRepository;
    @Mock FestivalLikeRepository festivalLikeRepository;
    @Mock FileStorageService fileStorageService;

    @InjectMocks FestivalServiceImpl festivalService;

    private Festival festival(Long id, String title, String posterKey) {
        return Festival.builder()
                .id(id).title(title).posterKey(posterKey)
                .startDate(LocalDate.of(2026, 8, 1)).endDate(LocalDate.of(2026, 8, 3))
                .region(Region.SEOUL)
                .build();
    }

    private FestivalRequestDto dto(String title, LocalDate start, LocalDate end) {
        return FestivalRequestDto.builder().title(title).startDate(start).endDate(end).build();
    }

    // ── createFestival ────────────────────────────────────────────────

    @Test
    void 종료일이_시작일보다_이전이면_예외() {
        FestivalRequestDto dto = dto("제목", LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 1));

        assertThatThrownBy(() -> festivalService.createFestival(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("종료일은 시작일보다 이전일 수 없습니다.");
    }

    // ── getFestivalDetail ─────────────────────────────────────────────

    @Test
    void 페스티벌_상세_조회_성공() {
        Festival f = festival(1L, "락페", "posters/1.jpg");
        given(festivalRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(f));
        given(fileStorageService.buildUrl("posters/1.jpg")).willReturn("https://cdn.example.com/posters/1.jpg");

        FestivalDetailResponseDto result = festivalService.getFestivalDetail(1L);

        assertThat(result.getTitle()).isEqualTo("락페");
    }

    @Test
    void 존재하지_않는_페스티벌_상세_조회시_예외() {
        given(festivalRepository.findByIdAndDeletedAtIsNull(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> festivalService.getFestivalDetail(99L))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── updateFestival: posterKey 변경 분기 ────────────────────────────

    @Test
    void 포스터_변경시_기존_포스터_S3_삭제() {
        Festival f = festival(1L, "락페", "posters/old.jpg");
        given(festivalRepository.findById(1L)).willReturn(Optional.of(f));
        FestivalRequestDto updateDto = FestivalRequestDto.builder()
                .title("락페").startDate(LocalDate.of(2026, 8, 1)).endDate(LocalDate.of(2026, 8, 3))
                .posterKey("posters/new.jpg").build();

        festivalService.updateFestival(1L, updateDto);

        verify(fileStorageService).deleteFileAfterCommit("posters/old.jpg");
    }

    @Test
    void 포스터_변경없으면_S3_삭제_스킵() {
        Festival f = festival(1L, "락페", "posters/same.jpg");
        given(festivalRepository.findById(1L)).willReturn(Optional.of(f));
        FestivalRequestDto updateDto = FestivalRequestDto.builder()
                .title("락페").startDate(LocalDate.of(2026, 8, 1)).endDate(LocalDate.of(2026, 8, 3))
                .posterKey("posters/same.jpg").build();

        festivalService.updateFestival(1L, updateDto);

        verify(fileStorageService, never()).deleteFileAfterCommit(any());
    }

    // ── deleteFestival / restoreFestival / getDeletedFestivals ─────────

    @Test
    void 삭제시_소프트_삭제만_수행하고_연관데이터는_보존() {
        Festival f = festival(1L, "락페", "posters/1.jpg");
        given(festivalRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(f));

        festivalService.deleteFestival(1L);

        assertThat(f.isDeleted()).isTrue();
        verify(festivalRepository, never()).deleteById(any());
        verify(fileStorageService, never()).deleteFileAfterCommit(any());
    }

    @Test
    void 존재하지_않는_페스티벌_삭제시_예외() {
        given(festivalRepository.findByIdAndDeletedAtIsNull(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> festivalService.deleteFestival(99L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void 복구시_repository_restoreById_위임() {
        festivalService.restoreFestival(1L);

        verify(festivalRepository).restoreById(1L);
    }

    @Test
    void 삭제된_페스티벌_목록_조회() {
        Festival deleted = festival(1L, "락페", null);
        deleted.softDelete();
        given(festivalRepository.findSoftDeleted()).willReturn(List.of(deleted));

        assertThat(festivalService.getDeletedFestivals()).extracting(FestivalResponseDto::getId).containsExactly(1L);
    }

    // ── uploadPosterFile ──────────────────────────────────────────────

    @Test
    void 포스터_파일_업로드시_fileStorageService에_위임() throws Exception {
        var file = mock(org.springframework.web.multipart.MultipartFile.class);
        LocalDate start = LocalDate.of(2026, 8, 1);
        given(fileStorageService.storeFestivalPoster(file, start)).willReturn("posters/new.jpg");

        String result = festivalService.uploadPosterFile(file, start);

        assertThat(result).isEqualTo("posters/new.jpg");
    }

    // ── getLikedFestivals ─────────────────────────────────────────────

    @Test
    void 좋아요한_페스티벌_목록_조회() {
        Festival f = festival(1L, "락페", null);
        FestivalLike like = FestivalLike.of(TestEntityFactory.user(10L), f);
        given(festivalLikeRepository.findByUserId(eq(10L), any(Pageable.class))).willReturn(List.of(like));

        List<FestivalResponseDto> result = festivalService.getLikedFestivals(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("락페");
    }

    // ── searchFestivals ───────────────────────────────────────────────

    @Test
    void 짧은_키워드는_LIKE_폴백_검색() {
        Festival f = festival(1L, "IU", null);
        given(festivalRepository.findByTitleKeywordPaged(anyString(), any(PageRequest.class)))
                .willReturn(new PageImpl<>(List.of(f)));

        List<FestivalResponseDto> result = festivalService.searchFestivals("IU");

        assertThat(result).hasSize(1);
    }

    @Test
    void 충분히_긴_키워드는_풀텍스트_검색() {
        Festival f = festival(1L, "락페스티벌", null);
        given(festivalRepository.findByTitleKeyword(eq("락페스티벌"), any(Integer.class)))
                .willReturn(List.of(f));

        List<FestivalResponseDto> result = festivalService.searchFestivals("락페스티벌");

        assertThat(result).hasSize(1);
    }

    // ── getFestivalsAdminPage ─────────────────────────────────────────

    @Test
    void 키워드_없으면_전체_페스티벌_최신순_조회() {
        Festival f = festival(1L, "락페", null);
        given(festivalRepository.findAllByDeletedAtIsNull(any(Pageable.class))).willReturn(new PageImpl<>(List.of(f)));

        Page<FestivalResponseDto> result = festivalService.getFestivalsAdminPage(null, 0, 20);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void 키워드_있으면_키워드_검색_페이지_조회() {
        Festival f = festival(1L, "락페", null);
        given(festivalRepository.findByTitleKeywordPaged(anyString(), any(PageRequest.class)))
                .willReturn(new PageImpl<>(List.of(f)));

        Page<FestivalResponseDto> result = festivalService.getFestivalsAdminPage("락페", 0, 20);

        assertThat(result.getContent()).hasSize(1);
    }

    // ── getTotalCount ─────────────────────────────────────────────────

    @Test
    void 전체_페스티벌_수_조회() {
        given(festivalRepository.countByDeletedAtIsNull()).willReturn(15L);

        assertThat(festivalService.getTotalCount()).isEqualTo(15L);
    }

    // ── getAllFestivals: 정렬 분기 ────────────────────────────────────

    @Test
    void 날짜_오름차순_정렬() {
        Festival earlier = festival(1L, "먼저", null);
        Festival later = Festival.builder().id(2L).title("나중").startDate(LocalDate.of(2026, 9, 1))
                .endDate(LocalDate.of(2026, 9, 3)).region(Region.SEOUL).build();
        given(festivalRepository.findByFilters(any(), any(), any(), any(), any())).willReturn(List.of(later, earlier));

        List<FestivalResponseDto> result = festivalService.getAllFestivals(
                new FestivalFilterCriteria(null, null, null, true, "date_asc"));

        assertThat(result).extracting(FestivalResponseDto::getTitle).containsExactly("먼저", "나중");
    }

    @Test
    void 날짜_내림차순_정렬() {
        Festival earlier = festival(1L, "먼저", null);
        Festival later = Festival.builder().id(2L).title("나중").startDate(LocalDate.of(2026, 9, 1))
                .endDate(LocalDate.of(2026, 9, 3)).region(Region.SEOUL).build();
        given(festivalRepository.findByFilters(any(), any(), any(), any(), any())).willReturn(List.of(earlier, later));

        List<FestivalResponseDto> result = festivalService.getAllFestivals(
                new FestivalFilterCriteria(null, null, null, true, "date_desc"));

        assertThat(result).extracting(FestivalResponseDto::getTitle).containsExactly("나중", "먼저");
    }

    // ── getAllFestivalsForAdmin / getAllActiveFestivalsForAdmin ────────

    @Test
    void 관리자용_진행중_페스티벌만_필터링() {
        Festival ended = Festival.builder().id(1L).title("종료됨")
                .startDate(LocalDate.now().minusDays(10)).endDate(LocalDate.now().minusDays(1))
                .region(Region.SEOUL).build();
        given(festivalRepository.findByFilters(any(), any(), any(), any(), any())).willReturn(List.of(ended));

        List<FestivalResponseDto> result = festivalService.getAllActiveFestivalsForAdmin();

        assertThat(result).isEmpty();
    }

    // ── getFestivalsPage ────────────────────────────────────────────────

    @Test
    void 페이지_조회_기본정렬은_startDate_오름차순() {
        Festival f = festival(1L, "락페", null);
        given(festivalRepository.findByFiltersPage(any(), any(), any(), any(), any()))
                .willReturn(new org.springframework.data.domain.PageImpl<>(List.of(f)));

        festivalService.getFestivalsPage(
                new FestivalFilterCriteria(null, null, null, true, null),
                org.springframework.data.domain.PageRequest.of(0, 20));

        org.mockito.ArgumentCaptor<org.springframework.data.domain.Pageable> captor =
                org.mockito.ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        then(festivalRepository).should().findByFiltersPage(any(), any(), any(), any(), captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("startDate").getDirection())
                .isEqualTo(org.springframework.data.domain.Sort.Direction.ASC);
    }

    @Test
    void 페이지_조회_date_desc_지정시_내림차순() {
        given(festivalRepository.findByFiltersPage(any(), any(), any(), any(), any()))
                .willReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        festivalService.getFestivalsPage(
                new FestivalFilterCriteria(null, null, null, true, "date_desc"),
                org.springframework.data.domain.PageRequest.of(0, 20));

        org.mockito.ArgumentCaptor<org.springframework.data.domain.Pageable> captor =
                org.mockito.ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        then(festivalRepository).should().findByFiltersPage(any(), any(), any(), any(), captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("startDate").getDirection())
                .isEqualTo(org.springframework.data.domain.Sort.Direction.DESC);
    }

    @Test
    void 페이지_조회_결과를_DTO로_매핑() {
        Festival f = festival(1L, "락페", null);
        given(festivalRepository.findByFiltersPage(any(), any(), any(), any(), any()))
                .willReturn(new org.springframework.data.domain.PageImpl<>(List.of(f)));

        Page<FestivalResponseDto> result = festivalService.getFestivalsPage(
                new FestivalFilterCriteria(null, null, null, true, null),
                org.springframework.data.domain.PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(FestivalResponseDto::getTitle).containsExactly("락페");
    }
}
