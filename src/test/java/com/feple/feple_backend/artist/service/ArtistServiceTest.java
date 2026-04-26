package com.feple.feple_backend.artist.service;

import com.feple.feple_backend.artist.dto.ArtistRequestDto;
import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.entity.ArtistGenre;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ArtistServiceTest {

    @Mock ArtistRepository artistRepository;
    @Mock FileStorageService fileStorageService;
    @Mock ArtistCascadeDeleteService cascadeDeleteService;

    @InjectMocks ArtistService artistService;

    // ── createArtist ──────────────────────────────────────────────

    @Test
    void 아티스트_생성_성공() {
        ArtistRequestDto dto = ArtistRequestDto.builder()
                .name("아이유")
                .nameEn("IU")
                .genre(ArtistGenre.BALLAD)
                .profileImageKey("artists/iu.jpg")
                .build();

        Artist saved = Artist.builder()
                .id(1L).name("아이유").nameEn("IU")
                .genre(ArtistGenre.BALLAD).profileImageKey("artists/iu.jpg")
                .build();

        given(artistRepository.save(any(Artist.class))).willReturn(saved);

        Long id = artistService.createArtist(dto);

        assertThat(id).isEqualTo(1L);
        verify(artistRepository).save(any(Artist.class));
    }

    // ── getArtistById ─────────────────────────────────────────────

    @Test
    void 아티스트_ID_조회_성공() {
        Artist artist = Artist.builder()
                .id(1L).name("BTS").profileImageKey("bts.jpg")
                .build();

        given(artistRepository.findById(1L)).willReturn(Optional.of(artist));
        given(fileStorageService.buildUrl("bts.jpg")).willReturn("https://cdn.example.com/bts.jpg");

        ArtistResponseDto result = artistService.getArtistById(1L);

        assertThat(result.getName()).isEqualTo("BTS");
        assertThat(result.getProfileImageUrl()).isEqualTo("https://cdn.example.com/bts.jpg");
    }

    @Test
    void 없는_아티스트_조회시_404_예외() {
        given(artistRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> artistService.getArtistById(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    // ── updateArtist ──────────────────────────────────────────────

    @Test
    void 아티스트_수정_이미지_변경() {
        Artist artist = Artist.builder()
                .id(1L).name("아이유").nameEn("IU")
                .genre(ArtistGenre.BALLAD).profileImageKey("old.jpg")
                .build();

        ArtistRequestDto dto = ArtistRequestDto.builder()
                .name("아이유").nameEn("IU")
                .genre(ArtistGenre.BALLAD)
                .profileImageKey("new.jpg")
                .build();

        given(artistRepository.findById(1L)).willReturn(Optional.of(artist));

        artistService.updateArtist(1L, dto);

        // 기존 이미지 삭제 호출 검증
        verify(fileStorageService).deleteFile("old.jpg");
    }

    @Test
    void 아티스트_수정_이미지_없으면_기존_유지() {
        Artist artist = Artist.builder()
                .id(1L).name("아이유").nameEn("IU")
                .genre(ArtistGenre.BALLAD).profileImageKey("old.jpg")
                .build();

        // profileImageKey null → 이미지 변경 없음
        ArtistRequestDto dto = ArtistRequestDto.builder()
                .name("아이유 (수정)").nameEn("IU")
                .genre(ArtistGenre.BALLAD)
                .build();

        given(artistRepository.findById(1L)).willReturn(Optional.of(artist));

        artistService.updateArtist(1L, dto);

        // 삭제 호출 없어야 함
        verify(fileStorageService, never()).deleteFile(any());
    }

    @Test
    void 없는_아티스트_수정시_404_예외() {
        given(artistRepository.findById(99L)).willReturn(Optional.empty());

        ArtistRequestDto dto = ArtistRequestDto.builder().name("x").build();

        assertThatThrownBy(() -> artistService.updateArtist(99L, dto))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── deleteArtist ──────────────────────────────────────────────

    @Test
    void 아티스트_삭제_캐스케이드_호출() {
        Artist artist = Artist.builder()
                .id(1L).name("아이유").profileImageKey("iu.jpg")
                .build();

        given(artistRepository.findById(1L)).willReturn(Optional.of(artist));

        artistService.deleteArtist(1L);

        verify(cascadeDeleteService).delete(artist);
    }

    @Test
    void 없는_아티스트_삭제시_404_예외() {
        given(artistRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> artistService.deleteArtist(99L))
                .isInstanceOf(NoSuchElementException.class);

        verify(cascadeDeleteService, never()).delete(any());
    }

    // ── getAllArtists ─────────────────────────────────────────────

    @Test
    void 전체_아티스트_조회_URL_변환() {
        Artist a1 = Artist.builder().id(1L).name("아이유").profileImageKey("iu.jpg").build();
        Artist a2 = Artist.builder().id(2L).name("BTS").profileImageKey("bts.jpg").build();

        given(artistRepository.findAll(any(org.springframework.data.domain.PageRequest.class)))
                .willReturn(new PageImpl<>(List.of(a1, a2)));
        given(fileStorageService.buildUrl("iu.jpg")).willReturn("https://cdn/iu.jpg");
        given(fileStorageService.buildUrl("bts.jpg")).willReturn("https://cdn/bts.jpg");

        List<ArtistResponseDto> result = artistService.getAllArtists();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getProfileImageUrl()).isEqualTo("https://cdn/iu.jpg");
    }

    // ── searchArtists ─────────────────────────────────────────────

    @Test
    void 키워드_검색_결과_반환() {
        Artist artist = Artist.builder().id(1L).name("아이유").profileImageKey("iu.jpg").build();
        given(artistRepository.findByNameContainingIgnoreCaseOrderByNameAsc("아이"))
                .willReturn(List.of(artist));
        given(fileStorageService.buildUrl("iu.jpg")).willReturn("https://cdn/iu.jpg");

        List<ArtistResponseDto> result = artistService.searchArtists("아이");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("아이유");
    }

    @Test
    void 키워드_검색_결과_없으면_빈_리스트() {
        given(artistRepository.findByNameContainingIgnoreCaseOrderByNameAsc("없는가수"))
                .willReturn(List.of());

        List<ArtistResponseDto> result = artistService.searchArtists("없는가수");

        assertThat(result).isEmpty();
    }
}
