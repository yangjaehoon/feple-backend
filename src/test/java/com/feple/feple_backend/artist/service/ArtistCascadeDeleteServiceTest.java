package com.feple.feple_backend.artist.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.photo.repository.ArtistGalleryPhotoLikeRepository;
import com.feple.feple_backend.artist.photo.repository.ArtistGalleryPhotoRepository;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artist.song.repository.ArtistFestivalSongRepository;
import com.feple.feple_backend.artist.song.repository.SongRepository;
import com.feple.feple_backend.artist.song.repository.SongRequestRepository;
import com.feple.feple_backend.artistfestival.repository.ArtistFestivalRepository;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.post.service.PostCascadeDeleteService;
import com.feple.feple_backend.timetable.service.TimetableService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ArtistCascadeDeleteServiceTest {

    @Mock ArtistRepository artistRepository;
    @Mock ArtistGalleryPhotoRepository galleryPhotoRepository;
    @Mock ArtistGalleryPhotoLikeRepository galleryPhotoLikeRepository;
    @Mock ArtistFestivalRepository artistFestivalRepository;
    @Mock ArtistFestivalSongRepository artistFestivalSongRepository;
    @Mock ArtistFollowRepository artistFollowRepository;
    @Mock SongRepository songRepository;
    @Mock SongRequestRepository songRequestRepository;
    @Mock TimetableService timetableService;
    @Mock PostCascadeDeleteService postCascadeService;
    @Mock FileStorageService fileStorageService;

    @InjectMocks ArtistCascadeDeleteService artistCascadeDeleteService;

    private Artist artist(Long id) {
        return Artist.builder()
                .id(id).name("아티스트" + id)
                .profileImageKey("profile/artist-" + id + ".jpg")
                .build();
    }

    @Test
    void 아티스트_삭제시_갤러리_사진_좋아요와_사진_삭제됨() {
        Artist artist = artist(1L);
        given(artistFestivalRepository.findByArtistIdOrderByFestivalStartDateAsc(1L)).willReturn(List.of());

        artistCascadeDeleteService.delete(artist);

        verify(galleryPhotoLikeRepository).deleteByArtistId(1L);
        verify(galleryPhotoRepository).deleteByArtistId(1L);
    }

    @Test
    void 아티스트_삭제시_페스티벌연결_팔로우_게시글_삭제됨() {
        Artist artist = artist(1L);
        given(artistFestivalRepository.findByArtistIdOrderByFestivalStartDateAsc(1L)).willReturn(List.of());

        artistCascadeDeleteService.delete(artist);

        verify(artistFestivalRepository).deleteByArtistId(1L);
        verify(artistFollowRepository).deleteByArtistId(1L);
        verify(postCascadeService).deletePostsByArtist(artist);
    }

    @Test
    void 아티스트_삭제시_아티스트_레코드와_프로필_이미지_파일_삭제됨() {
        Artist artist = artist(1L);
        given(artistFestivalRepository.findByArtistIdOrderByFestivalStartDateAsc(1L)).willReturn(List.of());

        artistCascadeDeleteService.delete(artist);

        verify(artistRepository).deleteById(1L);
        verify(fileStorageService).deleteFileAfterCommit("profile/artist-1.jpg");
    }
}
