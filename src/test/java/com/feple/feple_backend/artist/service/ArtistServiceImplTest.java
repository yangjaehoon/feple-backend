package com.feple.feple_backend.artist.service;

import com.feple.feple_backend.artist.ArtistNameValidator;
import com.feple.feple_backend.artist.dto.ArtistRequestDto;
import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.dto.NameEnUpdate;
import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artist.song.repository.SongRepository;
import com.feple.feple_backend.artistfestival.entity.ArtistFestival;
import com.feple.feple_backend.artistfestival.repository.ArtistFestivalRepository;
import com.feple.feple_backend.artistfollow.entity.ArtistFollow;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.global.MusicGenre;
import com.feple.feple_backend.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ArtistServiceImplTest {

    @Mock ArtistRepository artistRepository;
    @Mock ArtistFollowRepository artistFollowRepository;
    @Mock ArtistFestivalRepository artistFestivalRepository;
    @Mock FileStorageService fileStorageService;
    @Mock ArtistCascadeDeleteService cascadeDeleteService;
    @Mock SongRepository songRepository;
    @Mock ArtistNameValidator artistNameValidator;

    @InjectMocks ArtistServiceImpl service;

    private Artist artist(Long id, String name) {
        return Artist.builder().id(id).name(name).nameEn(name + "En").followerCount(0).build();
    }

    // ── createArtist ─────────────────────────────────────────────────────

    @Test
    void 아티스트_생성시_저장후_이름검증기_갱신() {
        ArtistRequestDto dto = ArtistRequestDto.builder()
                .name("뉴진스").nameEn("NewJeans").aliases("엔진스, nj")
                .genres(List.of(MusicGenre.IDOL)).build();
        given(artistRepository.save(any(Artist.class))).willAnswer(inv -> {
            Artist a = inv.getArgument(0);
            return Artist.builder().id(1L).name(a.getName()).aliases(a.getAliases()).build();
        });

        Long id = service.createArtist(dto);

        assertThat(id).isEqualTo(1L);
        verify(artistNameValidator).reload();
    }

    @Test
    void 아티스트_생성시_별칭은_콤마로_분리되고_공백_항목_제외() {
        ArtistRequestDto dto = ArtistRequestDto.builder()
                .name("뉴진스").aliases(" 엔진스 , , nj ")
                .genres(List.of(MusicGenre.IDOL)).build();
        ArgumentCaptor<Artist> captor = ArgumentCaptor.forClass(Artist.class);
        given(artistRepository.save(captor.capture())).willReturn(artist(1L, "뉴진스"));

        service.createArtist(dto);

        assertThat(captor.getValue().getAliases()).containsExactly("엔진스", "nj");
    }

    // ── getAllArtistsSortedByName / getFollowedArtists / getAllArtists ────

    @Test
    void 이름순_전체조회는_repository_결과를_dto로_매핑() {
        given(artistRepository.findAll(Sort.by(Sort.Direction.ASC, "name")))
                .willReturn(List.of(artist(1L, "가수A")));

        List<ArtistResponseDto> result = service.getAllArtistsSortedByName();

        assertThat(result).extracting(ArtistResponseDto::getName).containsExactly("가수A");
    }

    @Test
    void 팔로우한_아티스트_목록_조회() {
        Artist a = artist(1L, "가수A");
        ArtistFollow follow = ArtistFollow.of(User.builder().id(10L).build(), a);
        given(artistFollowRepository.findByUserId(any(), any(Pageable.class))).willReturn(List.of(follow));

        List<ArtistResponseDto> result = service.getFollowedArtists(10L);

        assertThat(result).extracting(ArtistResponseDto::getId).containsExactly(1L);
    }

    // ── searchArtists ────────────────────────────────────────────────────

    @Test
    void 검색어가_3자_미만이면_LIKE_폴백_사용() {
        given(artistRepository.findByNameOrNameEnContainingIgnoreCase("iu"))
                .willReturn(List.of(artist(1L, "아이유")));

        List<ArtistResponseDto> result = service.searchArtists("iu");

        assertThat(result).hasSize(1);
        verify(artistRepository, never()).searchArtistsByNameFullText(any(), anyInt());
    }

    @Test
    void 검색어가_3자_이상이면_풀텍스트_검색_사용() {
        given(artistRepository.searchArtistsByNameFullText(eq("뉴진스"), anyInt()))
                .willReturn(List.of(artist(1L, "뉴진스")));

        List<ArtistResponseDto> result = service.searchArtists("뉴진스");

        assertThat(result).hasSize(1);
        verify(artistRepository, never()).findByNameOrNameEnContainingIgnoreCase(any());
    }

    // ── getAdminArtistList ───────────────────────────────────────────────

    @Test
    void 관리자_목록_키워드있으면_인메모리_처리() {
        given(songRepository.countGroupedByArtist()).willReturn(List.of());
        given(artistRepository.findByNameOrNameEnContainingIgnoreCase(any()))
                .willReturn(List.of(artist(1L, "뉴진스")));

        Page<ArtistResponseDto> result = service.getAdminArtistList(null, "뉴진스", null, 0);

        assertThat(result.getContent()).extracting(ArtistResponseDto::getName).containsExactly("뉴진스");
        verify(artistRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void 관리자_목록_songs_정렬이면_인메모리_처리후_곡수_내림차순() {
        given(songRepository.countGroupedByArtist())
                .willReturn(List.<Object[]>of(new Object[]{1L, 5L}, new Object[]{2L, 1L}));
        given(artistRepository.findAll()).willReturn(List.of(artist(1L, "A"), artist(2L, "B")));

        Page<ArtistResponseDto> result = service.getAdminArtistList("songs", null, null, 0);

        assertThat(result.getContent()).extracting(ArtistResponseDto::getName).containsExactly("A", "B");
    }

    @Test
    void 관리자_목록_장르필터_적용시_해당_장르만_포함() {
        Artist idol = artist(1L, "아이돌팀");
        idol.getGenres().add(MusicGenre.IDOL);
        given(songRepository.countGroupedByArtist()).willReturn(List.of());
        given(artistRepository.findByNameOrNameEnContainingIgnoreCase(any()))
                .willReturn(List.of(idol, artist(2L, "밴드팀")));

        Page<ArtistResponseDto> result = service.getAdminArtistList(null, "팀", MusicGenre.IDOL, 0);

        assertThat(result.getContent()).extracting(ArtistResponseDto::getName).containsExactly("아이돌팀");
    }

    @Test
    void 관리자_목록_기본은_DB_페이지네이션() {
        Page<Artist> page = new PageImpl<>(List.of(artist(1L, "A")));
        given(artistRepository.findAll(any(Pageable.class))).willReturn(page);
        given(songRepository.countGroupedByArtistIds(anyList())).willReturn(List.of());

        Page<ArtistResponseDto> result = service.getAdminArtistList(null, null, null, 0);

        assertThat(result.getContent()).hasSize(1);
        verify(artistRepository).findAll(any(Pageable.class));
    }

    @Test
    void 관리자_목록_기본_장르지정시_findByGenreName_사용() {
        Page<Artist> page = new PageImpl<>(List.of(artist(1L, "A")));
        given(artistRepository.findByGenreName(eq("IDOL"), any(Pageable.class))).willReturn(page);
        given(songRepository.countGroupedByArtistIds(anyList())).willReturn(List.of());

        Page<ArtistResponseDto> result = service.getAdminArtistList(null, null, MusicGenre.IDOL, 0);

        assertThat(result.getContent()).hasSize(1);
    }

    // ── getArtistById ────────────────────────────────────────────────────

    @Test
    void 단건조회_성공() {
        given(artistRepository.findById(1L)).willReturn(Optional.of(artist(1L, "가수A")));

        assertThat(service.getArtistById(1L).getName()).isEqualTo("가수A");
    }

    @Test
    void 단건조회_존재하지_않으면_예외() {
        given(artistRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getArtistById(1L)).isInstanceOf(NoSuchElementException.class);
    }

    // ── getArtistForEdit ─────────────────────────────────────────────────

    @Test
    void 수정용_조회는_아티스트_필드를_요청DTO로_매핑() {
        given(artistRepository.findById(1L)).willReturn(Optional.of(artist(1L, "가수A")));

        ArtistRequestDto dto = service.getArtistForEdit(1L);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("가수A");
    }

    // ── updateArtist ─────────────────────────────────────────────────────

    @Test
    void 수정시_이미지키_없으면_기존이미지_유지_삭제안함() {
        Artist a = artist(1L, "가수A");
        given(artistRepository.findById(1L)).willReturn(Optional.of(a));
        ArtistRequestDto dto = ArtistRequestDto.builder().name("변경됨").genres(List.of(MusicGenre.IDOL)).build();

        service.updateArtist(1L, dto);

        assertThat(a.getName()).isEqualTo("변경됨");
        verify(fileStorageService, never()).deleteFileAfterCommit(any());
        verify(artistNameValidator).reload();
    }

    @Test
    void 수정시_새이미지키_있고_기존키_있으면_기존파일_삭제() {
        Artist a = Artist.builder().id(1L).name("가수A").profileImageKey("old-key").build();
        given(artistRepository.findById(1L)).willReturn(Optional.of(a));
        ArtistRequestDto dto = ArtistRequestDto.builder().name("가수A").genres(List.of(MusicGenre.IDOL))
                .profileImageKey("new-key").build();

        service.updateArtist(1L, dto);

        assertThat(a.getProfileImageKey()).isEqualTo("new-key");
        verify(fileStorageService).deleteFileAfterCommit("old-key");
    }

    // ── getTopArtists ────────────────────────────────────────────────────

    @Test
    void 인기_아티스트_상위N개_조회() {
        Page<Artist> page = new PageImpl<>(List.of(artist(1L, "A")));
        given(artistRepository.findAll(any(Pageable.class))).willReturn(page);

        assertThat(service.getTopArtists(5)).extracting(ArtistResponseDto::getName).containsExactly("A");
    }

    // ── updateArtistPhoto ────────────────────────────────────────────────

    @Test
    void 프로필사진_변경시_기존키_있으면_삭제() {
        Artist a = Artist.builder().id(1L).name("A").profileImageKey("old").build();
        given(artistRepository.findById(1L)).willReturn(Optional.of(a));

        service.updateArtistPhoto(1L, "new");

        assertThat(a.getProfileImageKey()).isEqualTo("new");
        verify(fileStorageService).deleteFileAfterCommit("old");
    }

    @Test
    void 프로필사진_변경시_기존키_없으면_삭제호출없음() {
        Artist a = artist(1L, "A");
        given(artistRepository.findById(1L)).willReturn(Optional.of(a));

        service.updateArtistPhoto(1L, "new");

        verify(fileStorageService, never()).deleteFileAfterCommit(any());
    }

    // ── batchUpdateNameEn ────────────────────────────────────────────────

    @Test
    void 영문명_일괄수정_공백이면_null로_설정() {
        Artist a = artist(1L, "가수A");
        given(artistRepository.findAllById(List.of(1L))).willReturn(List.of(a));

        service.batchUpdateNameEn(List.of(new NameEnUpdate(1L, "  ")));

        assertThat(a.getNameEn()).isNull();
    }

    @Test
    void 영문명_일괄수정_존재하지_않는_ID는_무시() {
        given(artistRepository.findAllById(List.of(99L))).willReturn(List.of());

        service.batchUpdateNameEn(List.of(new NameEnUpdate(99L, "New")));
        // 예외 없이 조용히 무시됨
    }

    @Test
    void 영문명_일괄수정_정상값이면_trim되어_반영() {
        Artist a = artist(1L, "가수A");
        given(artistRepository.findAllById(List.of(1L))).willReturn(List.of(a));

        service.batchUpdateNameEn(List.of(new NameEnUpdate(1L, "  NewName  ")));

        assertThat(a.getNameEn()).isEqualTo("NewName");
    }

    // ── deleteArtist ─────────────────────────────────────────────────────

    @Test
    void 삭제시_cascade_위임후_이름검증기_갱신() {
        Artist a = artist(1L, "A");
        given(artistRepository.findById(1L)).willReturn(Optional.of(a));

        service.deleteArtist(1L);

        verify(cascadeDeleteService).delete(a);
        verify(artistNameValidator).reload();
    }

    // ── getRelatedArtists ────────────────────────────────────────────────

    @Test
    void 연관아티스트_출연_페스티벌_없으면_빈목록() {
        given(artistFestivalRepository.findByArtistIdOrderByFestivalStartDateAsc(1L)).willReturn(List.of());

        assertThat(service.getRelatedArtists(1L, 5)).isEmpty();
        verify(artistFestivalRepository, never()).findByFestivalIdInWithArtist(any());
    }

    @Test
    void 연관아티스트_공동출연_많은순으로_반환() {
        Artist target = artist(1L, "타깃");
        ArtistFestival af1 = artistFestival(target, festival(100L));
        given(artistFestivalRepository.findByArtistIdOrderByFestivalStartDateAsc(1L)).willReturn(List.of(af1));

        Artist co1 = artist(2L, "동반1");
        Artist co2 = artist(3L, "동반2");
        List<ArtistFestival> coAppearances = List.of(
                artistFestival(co1, festival(100L)),
                artistFestival(co2, festival(100L)),
                artistFestival(co2, festival(101L))
        );
        given(artistFestivalRepository.findByFestivalIdInWithArtist(List.of(100L))).willReturn(coAppearances);
        given(artistRepository.findAllById(List.of(3L, 2L))).willReturn(List.of(co2, co1));

        List<ArtistResponseDto> result = service.getRelatedArtists(1L, 5);

        assertThat(result).extracting(ArtistResponseDto::getId).containsExactly(3L, 2L);
    }

    private com.feple.feple_backend.festival.entity.Festival festival(Long id) {
        return com.feple.feple_backend.festival.entity.Festival.builder().id(id).build();
    }

    private ArtistFestival artistFestival(Artist artist, com.feple.feple_backend.festival.entity.Festival festival) {
        return ArtistFestival.builder().artist(artist).festival(festival).build();
    }

    // ── getTotalCount ────────────────────────────────────────────────────

    @Test
    void 전체_아티스트_수_위임() {
        given(artistRepository.count()).willReturn(42L);

        assertThat(service.getTotalCount()).isEqualTo(42L);
    }
}
