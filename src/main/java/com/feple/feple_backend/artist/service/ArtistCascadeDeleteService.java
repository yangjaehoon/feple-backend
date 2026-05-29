package com.feple.feple_backend.artist.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.photo.entity.ArtistProfileImage;
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

@Service
@RequiredArgsConstructor
public class ArtistCascadeDeleteService {

    private final ArtistRepository artistRepository;
    private final ArtistProfileImageRepository artistImageRepository;
    private final ArtistFestivalRepository artistFestivalRepository;
    private final ArtistFollowRepository artistFollowRepository;
    private final PostCascadeService postCascadeService;
    private final FileStorageService fileStorageService;

    @Transactional
    public void delete(Artist artist) {
        String profileImageKey = artist.getProfileImageKey();

        List<ArtistProfileImage> images = artistImageRepository.findByArtist(artist);
        images.forEach(img -> fileStorageService.deleteFile(img.getImageUrl()));
        artistImageRepository.deleteAll(images);

        artistFestivalRepository.deleteByArtistId(artist.getId());
        artistFollowRepository.deleteAll(artistFollowRepository.findByArtistId(artist.getId()));

        postCascadeService.deletePostsByArtist(artist);

        artistRepository.deleteById(artist.getId());
        fileStorageService.deleteFile(profileImageKey);
    }
}
