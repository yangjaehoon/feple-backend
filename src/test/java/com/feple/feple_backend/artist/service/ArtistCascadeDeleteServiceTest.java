package com.feple.feple_backend.artist.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.photo.entity.ArtistProfileImage;
import com.feple.feple_backend.artist.photo.repository.ArtistProfileImageLikeRepository;
import com.feple.feple_backend.artist.photo.repository.ArtistProfileImageRepository;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artist.song.repository.ArtistFestivalSongRepository;
import com.feple.feple_backend.artist.song.repository.SongRepository;
import com.feple.feple_backend.artist.song.repository.SongRequestRepository;
import com.feple.feple_backend.artistfestival.repository.ArtistFestivalRepository;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.post.service.PostCascadeService;
import com.feple.feple_backend.timetable.service.TimetableService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ArtistCascadeDeleteServiceTest {

    @Mock ArtistRepository artistRepository;
    @Mock ArtistProfileImageRepository artistImageRepository;
    @Mock ArtistProfileImageLikeRepository artistImageLikeRepository;
    @Mock ArtistFestivalRepository artistFestivalRepository;
    @Mock ArtistFestivalSongRepository artistFestivalSongRepository;
    @Mock ArtistFollowRepository artistFollowRepository;
    @Mock SongRepository songRepository;
    @Mock SongRequestRepository songRequestRepository;
    @Mock TimetableService timetableService;
    @Mock PostCascadeService postCascadeService;
    @Mock FileStorageService fileStorageService;

    @InjectMocks ArtistCascadeDeleteService artistCascadeDeleteService;

    private Artist artist(Long id) {
        return Artist.builder()
                .id(id).name("아티스트" + id)
                .profileImageKey("profile/artist-" + id + ".jpg")
                .build();
    }

    private ArtistProfileImage galleryImage(Long id, String imageKey) {
        return ArtistProfileImage.builder().id(id).imageKey(imageKey).build();
    }

    @Test
    void 아티스트_삭제시_갤러리_이미지_파일과_DB레코드_삭제됨() {
        Artist artist = artist(1L);
        ArtistProfileImage img1 = galleryImage(1L, "gallery/img1.jpg");
        ArtistProfileImage img2 = galleryImage(2L, "gallery/img2.jpg");
        List<ArtistProfileImage> images = List.of(img1, img2);

        given(artistImageRepository.findByArtist(artist)).willReturn(images);
        given(artistFestivalRepository.findByArtistIdOrderByFestivalStartDateAsc(1L)).willReturn(List.of());

        artistCascadeDeleteService.delete(artist);

        verify(fileStorageService).deleteFileAfterCommit("gallery/img1.jpg");
        verify(fileStorageService).deleteFileAfterCommit("gallery/img2.jpg");
        verify(artistImageLikeRepository).deleteByArtistProfileImageIdIn(List.of(1L, 2L));
        verify(artistImageRepository).deleteAllInBatch(images);
    }

    @Test
    void 아티스트_삭제시_페스티벌연결_팔로우_게시글_삭제됨() {
        Artist artist = artist(1L);
        given(artistImageRepository.findByArtist(artist)).willReturn(List.of());
        given(artistFestivalRepository.findByArtistIdOrderByFestivalStartDateAsc(1L)).willReturn(List.of());

        artistCascadeDeleteService.delete(artist);

        verify(artistFestivalRepository).deleteByArtistId(1L);
        verify(artistFollowRepository).deleteByArtistId(1L);
        verify(postCascadeService).deletePostsByArtist(artist);
    }

    @Test
    void 아티스트_삭제시_아티스트_레코드와_프로필_이미지_파일_삭제됨() {
        Artist artist = artist(1L);
        given(artistImageRepository.findByArtist(artist)).willReturn(List.of());
        given(artistFestivalRepository.findByArtistIdOrderByFestivalStartDateAsc(1L)).willReturn(List.of());

        artistCascadeDeleteService.delete(artist);

        verify(artistRepository).deleteById(1L);
        verify(fileStorageService).deleteFileAfterCommit("profile/artist-1.jpg");
    }

    @Test
    void 갤러리_이미지_없는_아티스트도_정상_삭제됨() {
        Artist artist = artist(1L);
        given(artistImageRepository.findByArtist(artist)).willReturn(List.of());
        given(artistFestivalRepository.findByArtistIdOrderByFestivalStartDateAsc(1L)).willReturn(List.of());

        artistCascadeDeleteService.delete(artist);

        verify(artistImageLikeRepository, never()).deleteByArtistProfileImageIdIn(any());
        verify(artistImageRepository).deleteAllInBatch(List.of());
        verify(artistRepository).deleteById(1L);
    }

    @Test
    void 갤러리_이미지_없을때_파일_삭제는_프로필_이미지만_호출됨() {
        Artist artist = artist(1L);
        given(artistImageRepository.findByArtist(artist)).willReturn(List.of());
        given(artistFestivalRepository.findByArtistIdOrderByFestivalStartDateAsc(1L)).willReturn(List.of());

        artistCascadeDeleteService.delete(artist);

        verify(fileStorageService, never()).deleteFileAfterCommit("gallery/img1.jpg");
        verify(fileStorageService).deleteFileAfterCommit("profile/artist-1.jpg");
    }
}
