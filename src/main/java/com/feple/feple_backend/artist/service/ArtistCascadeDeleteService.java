package com.feple.feple_backend.artist.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.photo.entity.ArtistProfileImage;
import com.feple.feple_backend.artist.photo.entity.ArtistGalleryPhoto;
import com.feple.feple_backend.artist.photo.repository.ArtistGalleryPhotoLikeRepository;
import com.feple.feple_backend.artist.photo.repository.ArtistGalleryPhotoRepository;
import com.feple.feple_backend.artist.photo.repository.ArtistProfileImageLikeRepository;
import com.feple.feple_backend.artist.photo.repository.ArtistProfileImageRepository;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artist.song.repository.ArtistFestivalSongRepository;
import com.feple.feple_backend.artist.song.repository.SongRepository;
import com.feple.feple_backend.artist.song.repository.SongRequestRepository;
import com.feple.feple_backend.artistfestival.entity.ArtistFestival;
import com.feple.feple_backend.artistfestival.repository.ArtistFestivalRepository;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.post.service.PostCascadeService;
import com.feple.feple_backend.timetable.service.TimetableService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArtistCascadeDeleteService {

    private final ArtistRepository artistRepository;
    private final ArtistGalleryPhotoRepository galleryPhotoRepository;
    private final ArtistGalleryPhotoLikeRepository galleryPhotoLikeRepository;
    private final ArtistProfileImageRepository artistImageRepository;
    private final ArtistProfileImageLikeRepository artistImageLikeRepository;
    private final ArtistFestivalRepository artistFestivalRepository;
    private final ArtistFestivalSongRepository artistFestivalSongRepository;
    private final ArtistFollowRepository artistFollowRepository;
    private final SongRepository songRepository;
    private final SongRequestRepository songRequestRepository;
    private final TimetableService timetableService;
    private final PostCascadeService postCascadeService;
    private final FileStorageService fileStorageService;

    @Transactional
    public void delete(Artist artist) {
        String profileImageKey = artist.getProfileImageKey();

        // 갤러리 사진 좋아요 → 갤러리 사진 (artist_photos FK 정리)
        List<String> galleryPhotoKeys = galleryPhotoRepository.findByArtist_IdOrderByIdDesc(artist.getId())
                .stream().map(ArtistGalleryPhoto::getS3Key).toList();
        galleryPhotoLikeRepository.deleteByArtistId(artist.getId());
        galleryPhotoRepository.deleteByArtistId(artist.getId());

        // 프로필 이미지 좋아요 → 프로필 이미지
        List<ArtistProfileImage> images = artistImageRepository.findByArtist(artist);
        List<String> profileImageKeys = images.stream().map(ArtistProfileImage::getImageKey).toList();
        if (!images.isEmpty()) {
            List<Long> imageIds = images.stream().map(ArtistProfileImage::getId).collect(Collectors.toList());
            artistImageLikeRepository.deleteByArtistProfileImageIdIn(imageIds);
        }
        artistImageRepository.deleteAllInBatch(images);

        List<Long> artistFestivalIds = artistFestivalRepository.findByArtistIdOrderByFestivalStartDateAsc(artist.getId())
                .stream().map(ArtistFestival::getId).toList();
        if (!artistFestivalIds.isEmpty()) {
            artistFestivalSongRepository.deleteByArtistFestivalIdIn(artistFestivalIds);
        }
        artistFestivalRepository.deleteByArtistId(artist.getId());

        songRequestRepository.deleteByArtistId(artist.getId());
        songRepository.deleteByArtistId(artist.getId());

        timetableService.nullifyArtistId(artist.getId());
        artistFollowRepository.deleteByArtistId(artist.getId());

        postCascadeService.deletePostsByArtist(artist);

        artistRepository.deleteById(artist.getId());

        galleryPhotoKeys.forEach(fileStorageService::deleteFileAfterCommit);
        profileImageKeys.forEach(fileStorageService::deleteFileAfterCommit);
        fileStorageService.deleteFileAfterCommit(profileImageKey);
    }
}
