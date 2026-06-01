package com.feple.feple_backend.artist.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.photo.entity.ArtistProfileImage;
import com.feple.feple_backend.artist.photo.repository.ArtistProfileImageLikeRepository;
import com.feple.feple_backend.artist.photo.repository.ArtistProfileImageRepository;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artistfestival.repository.ArtistFestivalRepository;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.post.service.PostCascadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArtistCascadeDeleteService {

    private final ArtistRepository artistRepository;
    private final ArtistProfileImageRepository artistImageRepository;
    private final ArtistProfileImageLikeRepository artistImageLikeRepository;
    private final ArtistFestivalRepository artistFestivalRepository;
    private final ArtistFollowRepository artistFollowRepository;
    private final PostCascadeService postCascadeService;
    private final FileStorageService fileStorageService;

    @Transactional
    public void delete(Artist artist) {
        String profileImageKey = artist.getProfileImageKey();

        List<ArtistProfileImage> images = artistImageRepository.findByArtist(artist);
        images.forEach(img -> fileStorageService.deleteFile(img.getImageKey()));
        if (!images.isEmpty()) {
            List<Long> imageIds = images.stream().map(ArtistProfileImage::getId).collect(Collectors.toList());
            artistImageLikeRepository.deleteByArtistProfileImageIdIn(imageIds);
        }
        artistImageRepository.deleteAll(images);

        artistFestivalRepository.deleteByArtistId(artist.getId());
        artistFollowRepository.deleteAll(artistFollowRepository.findByArtistId(artist.getId()));

        postCascadeService.deletePostsByArtist(artist);

        artistRepository.deleteById(artist.getId());
        fileStorageService.deleteFile(profileImageKey);
    }
}
