package com.feple.feple_backend.artist.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.photo.repository.ArtistGalleryPhotoLikeRepository;
import com.feple.feple_backend.artist.photo.repository.ArtistGalleryPhotoRepository;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artist.song.repository.SongRepository;
import com.feple.feple_backend.artist.song.repository.SongRequestRepository;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import com.feple.feple_backend.artistfollow.service.ArtistFollowService;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.post.service.PostCascadeDeleteService;
import com.feple.feple_backend.timetable.service.TimetableService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ArtistCascadeDeleteServiceTest {

    @Mock ArtistRepository artistRepository;
    @Mock ArtistGalleryPhotoRepository galleryPhotoRepository;
    @Mock ArtistGalleryPhotoLikeRepository galleryPhotoLikeRepository;
    @Mock ArtistFestivalService artistFestivalService;
    @Mock ArtistFollowService artistFollowService;
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

        artistCascadeDeleteService.delete(artist);

        verify(galleryPhotoLikeRepository).deleteByArtistId(1L);
        verify(galleryPhotoRepository).deleteByArtistId(1L);
    }

    @Test
    void 아티스트_삭제시_페스티벌연결_팔로우_게시글_삭제됨() {
        Artist artist = artist(1L);

        artistCascadeDeleteService.delete(artist);

        verify(artistFestivalService).removeAllByArtist(1L);
        verify(artistFollowService).removeAllByArtist(1L);
        verify(postCascadeService).deletePostsByArtist(artist);
    }

    @Test
    void 아티스트_삭제시_아티스트_레코드와_프로필_이미지_파일_삭제됨() {
        Artist artist = artist(1L);

        artistCascadeDeleteService.delete(artist);

        verify(artistRepository).deleteById(1L);
        verify(fileStorageService).deleteFileAfterCommit("profile/artist-1.jpg");
    }
}
