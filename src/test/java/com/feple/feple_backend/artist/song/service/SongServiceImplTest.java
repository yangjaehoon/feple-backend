package com.feple.feple_backend.artist.song.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artist.song.dto.FestivalSetlistEntryDto;
import com.feple.feple_backend.artist.song.dto.SaveSongRequestDto;
import com.feple.feple_backend.artist.song.dto.SongFestivalDto;
import com.feple.feple_backend.artist.song.dto.SongResponseDto;
import com.feple.feple_backend.artist.song.entity.ArtistFestivalSong;
import com.feple.feple_backend.artist.song.entity.Song;
import com.feple.feple_backend.artist.song.repository.ArtistFestivalSongRepository;
import com.feple.feple_backend.artist.song.repository.SongRepository;
import com.feple.feple_backend.artistfestival.entity.ArtistFestival;
import com.feple.feple_backend.artistfestival.repository.ArtistFestivalRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class SongServiceImplTest {

    @Mock SongRepository songRepository;
    @Mock ArtistRepository artistRepository;
    @Mock ArtistFestivalRepository artistFestivalRepository;
    @Mock ArtistFestivalSongRepository artistFestivalSongRepository;
    @Mock YoutubeSearchService youtubeSearchService;
    @Mock FileStorageService fileStorageService;

    @InjectMocks SongServiceImpl service;

    private Artist artist(Long id) {
        return Artist.builder().id(id).name("아티스트" + id).build();
    }

    private Song song(Long id, String title, Artist artist) {
        return Song.builder().id(id).title(title).youtubeVideoId("yt" + id).artist(artist).build();
    }

    // ── getSongsByArtistId ────────────────────────────────────────────────

    @Test
    void 아티스트_곡_목록_없으면_빈리스트() {
        given(songRepository.findByArtistIdOrderByIdDesc(1L)).willReturn(List.of());

        assertThat(service.getSongsByArtistId(1L)).isEmpty();
        then(artistFestivalSongRepository).shouldHaveNoInteractions();
    }

    @Test
    void 아티스트_곡_목록_페스티벌횟수_내림차순_정렬() {
        Artist artist = artist(1L);
        Song songA = song(1L, "B곡", artist);
        Song songB = song(2L, "A곡", artist);
        given(songRepository.findByArtistIdOrderByIdDesc(1L)).willReturn(List.of(songA, songB));
        given(artistFestivalSongRepository.countGroupedBySongForArtist(1L))
                .willReturn(List.<Object[]>of(new Object[]{1L, 1L}, new Object[]{2L, 3L}));

        List<SongResponseDto> result = service.getSongsByArtistId(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTitle()).isEqualTo("A곡");
        assertThat(result.get(0).getFestivalCount()).isEqualTo(3);
        assertThat(result.get(1).getTitle()).isEqualTo("B곡");
    }

    // ── getSongFestivals ──────────────────────────────────────────────────

    @Test
    void 곡의_페스티벌_목록_조회() {
        ArtistFestivalSong afs = mock(ArtistFestivalSong.class);
        given(afs.getFestivalId()).willReturn(10L);
        given(afs.getFestivalTitle()).willReturn("펜타포트");
        given(afs.getFestivalStartDate()).willReturn(java.time.LocalDate.of(2026, 8, 1));
        given(artistFestivalSongRepository.findBySongIdWithFestival(1L)).willReturn(List.of(afs));

        List<SongFestivalDto> result = service.getSongFestivals(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFestivalTitle()).isEqualTo("펜타포트");
        assertThat(result.get(0).getStartDate()).isEqualTo("2026-08-01");
    }

    // ── searchYoutube / fetchVideoByUrl (위임) ────────────────────────────

    @Test
    void 유튜브_검색은_YoutubeSearchService에_위임() {
        service.searchYoutube("아이유", "좋은날");

        then(youtubeSearchService).should().search("아이유", "좋은날");
    }

    @Test
    void 유튜브_URL로_영상조회는_YoutubeSearchService에_위임() {
        service.fetchVideoByUrl("https://youtu.be/abc");

        then(youtubeSearchService).should().fetchVideoByUrl("https://youtu.be/abc");
    }

    // ── saveSong ──────────────────────────────────────────────────────────

    @Test
    void 곡_저장_성공() {
        Artist artist = artist(1L);
        given(artistRepository.findById(1L)).willReturn(Optional.of(artist));
        given(songRepository.existsByYoutubeVideoIdAndArtistId("yt1", 1L)).willReturn(false);
        given(songRepository.save(any())).willReturn(song(100L, "제목", artist));

        SaveSongRequestDto dto = new SaveSongRequestDto();
        dto.setYoutubeVideoId("yt1");
        dto.setTitle("제목");

        SongResponseDto result = service.saveSong(1L, dto);

        assertThat(result.getId()).isEqualTo(100L);
    }

    @Test
    void 곡_저장_이미_등록된_곡이면_예외() {
        given(artistRepository.findById(1L)).willReturn(Optional.of(artist(1L)));
        given(songRepository.existsByYoutubeVideoIdAndArtistId("yt1", 1L)).willReturn(true);

        SaveSongRequestDto dto = new SaveSongRequestDto();
        dto.setYoutubeVideoId("yt1");
        dto.setTitle("제목");

        assertThatThrownBy(() -> service.saveSong(1L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 등록된 곡입니다.");
    }

    @Test
    void 곡_저장_아티스트_없으면_예외() {
        given(artistRepository.findById(1L)).willReturn(Optional.empty());

        SaveSongRequestDto dto = new SaveSongRequestDto();
        dto.setYoutubeVideoId("yt1");
        dto.setTitle("제목");

        assertThatThrownBy(() -> service.saveSong(1L, dto))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── deleteSong ────────────────────────────────────────────────────────

    @Test
    void 곡_삭제_성공() {
        Song song = song(1L, "제목", artist(10L));
        given(songRepository.findById(1L)).willReturn(Optional.of(song));

        service.deleteSong(10L, 1L);

        then(songRepository).should().delete(song);
    }

    @Test
    void 곡_삭제_다른_아티스트_곡이면_예외() {
        Song song = song(1L, "제목", artist(10L));
        given(songRepository.findById(1L)).willReturn(Optional.of(song));

        assertThatThrownBy(() -> service.deleteSong(999L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 아티스트의 곡이 아닙니다.");
        then(songRepository).should(never()).delete(any());
    }

    // ── getFestivalSetlist ────────────────────────────────────────────────

    @Test
    void 페스티벌_셋리스트_참여아티스트_없으면_빈리스트() {
        given(artistFestivalRepository.findByFestivalIdOrderByLineupOrderAsc(1L)).willReturn(List.of());

        assertThat(service.getFestivalSetlist(1L)).isEmpty();
        then(artistFestivalSongRepository).shouldHaveNoInteractions();
    }

    @Test
    void 페스티벌_셋리스트_아티스트별로_곡_그룹핑() {
        ArtistFestival af = mock(ArtistFestival.class);
        given(af.getId()).willReturn(5L);
        given(af.getArtistId()).willReturn(1L);
        given(af.getArtistName()).willReturn("아이유");
        given(artistFestivalRepository.findByFestivalIdOrderByLineupOrderAsc(1L)).willReturn(List.of(af));

        ArtistFestivalSong afs = mock(ArtistFestivalSong.class);
        given(afs.getArtistFestivalId()).willReturn(5L);
        given(afs.getSong()).willReturn(song(1L, "좋은날", artist(1L)));
        given(artistFestivalSongRepository.findByFestivalIdWithDetails(1L)).willReturn(List.of(afs));

        List<FestivalSetlistEntryDto> result = service.getFestivalSetlist(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getArtistName()).isEqualTo("아이유");
        assertThat(result.get(0).getSongs()).hasSize(1);
        assertThat(result.get(0).getSongs().get(0).getTitle()).isEqualTo("좋은날");
    }

    // ── getSetlistCounts ──────────────────────────────────────────────────

    @Test
    void 셋리스트_카운트_빈리스트면_쿼리없이_빈맵() {
        assertThat(service.getSetlistCounts(List.of())).isEmpty();
        then(artistFestivalSongRepository).shouldHaveNoInteractions();
    }

    @Test
    void 셋리스트_카운트_조회() {
        given(artistFestivalSongRepository.countGroupedByArtistFestivalIds(List.of(1L)))
                .willReturn(List.<Object[]>of(new Object[]{1L, 3L}));

        assertThat(service.getSetlistCounts(List.of(1L))).containsEntry(1L, 3);
    }

    // ── getSetlist ────────────────────────────────────────────────────────

    @Test
    void 셋리스트_조회_위임() {
        List<ArtistFestivalSong> list = List.of(mock(ArtistFestivalSong.class));
        given(artistFestivalSongRepository.findByArtistFestivalId(5L)).willReturn(list);

        assertThat(service.getSetlist(5L)).isEqualTo(list);
    }

    // ── updateSetlist / saveSetlist ───────────────────────────────────────

    @Test
    void 셋리스트_수정_다른_페스티벌이면_예외() {
        ArtistFestival af = mock(ArtistFestival.class);
        given(af.getFestivalId()).willReturn(999L);
        given(artistFestivalRepository.findById(5L)).willReturn(Optional.of(af));

        assertThatThrownBy(() -> service.updateSetlist(1L, 5L, Set.of(1L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 아티스트는 이 페스티벌에 참여하지 않습니다.");
    }

    @Test
    void 셋리스트_수정_성공시_기존_삭제_후_재저장() {
        ArtistFestival af = mock(ArtistFestival.class);
        given(af.getFestivalId()).willReturn(1L);
        given(af.getId()).willReturn(5L);
        given(artistFestivalRepository.findById(5L)).willReturn(Optional.of(af));
        given(songRepository.findAllById(Set.of(1L, 2L))).willReturn(List.of(song(1L, "곡1", artist(1L))));

        service.updateSetlist(1L, 5L, Set.of(1L, 2L));

        then(artistFestivalSongRepository).should().deleteByArtistFestivalId(5L);
        then(artistFestivalSongRepository).should().saveAll(any());
    }

    @Test
    void 셋리스트_저장_songIds_비어있으면_삭제만_수행() {
        ArtistFestival af = mock(ArtistFestival.class);
        given(af.getId()).willReturn(5L);
        given(artistFestivalRepository.findById(5L)).willReturn(Optional.of(af));

        service.saveSetlist(5L, Set.of());

        then(artistFestivalSongRepository).should().deleteByArtistFestivalId(5L);
        then(songRepository).should(never()).findAllById(any());
    }

    @Test
    void 셋리스트_저장_songIds_null이면_삭제만_수행() {
        ArtistFestival af = mock(ArtistFestival.class);
        given(af.getId()).willReturn(5L);
        given(artistFestivalRepository.findById(5L)).willReturn(Optional.of(af));

        service.saveSetlist(5L, null);

        then(artistFestivalSongRepository).should().deleteByArtistFestivalId(5L);
        then(artistFestivalSongRepository).should(never()).saveAll(any());
    }
}
