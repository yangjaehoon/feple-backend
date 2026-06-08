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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
        List<String> galleryKeys = images.stream().map(ArtistProfileImage::getImageKey).toList();
        if (!images.isEmpty()) {
            List<Long> imageIds = images.stream().map(ArtistProfileImage::getId).collect(Collectors.toList());
            artistImageLikeRepository.deleteByArtistProfileImageIdIn(imageIds);
        }
        artistImageRepository.deleteAll(images);

        artistFestivalRepository.deleteByArtistId(artist.getId());
        artistFollowRepository.deleteAll(artistFollowRepository.findByArtistId(artist.getId()));

        postCascadeService.deletePostsByArtist(artist);

        artistRepository.deleteById(artist.getId());

        // S3 삭제는 커밋 후 실행 — 롤백 시 파일이 남아 있어야 하고, 커넥션을 불필요하게 점유하지 않도록 함
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                galleryKeys.forEach(fileStorageService::deleteFile);
                fileStorageService.deleteFile(profileImageKey);
            }
        });
    }
}
