package com.feple.feple_backend.artist.photo.service;

import com.feple.feple_backend.artist.photo.entity.ArtistProfileImage;
import com.feple.feple_backend.artist.photo.entity.ArtistProfileImageLike;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.artist.photo.repository.ArtistProfileImageLikeRepository;
import com.feple.feple_backend.artist.photo.repository.ArtistProfileImageRepository;
import com.feple.feple_backend.global.EntityFinder;
import com.feple.feple_backend.user.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArtistProfileImageLikeService {
    private final ArtistProfileImageLikeRepository likeRepository;
    private final ArtistProfileImageRepository imageRepository;
    private final UserRepository userRepository;

    @Transactional
    public void likeImage(Long imageId, Long userId) {
        ArtistProfileImage image = EntityFinder.getOrThrow(imageRepository::findById, imageId, "이미지");
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");

        likeRepository.findByUserAndArtistProfileImage(user, image)
                .ifPresentOrElse(
                        like -> {},
                        () -> likeRepository.save(ArtistProfileImageLike.builder()
                                .user(user)
                                .artistProfileImage(image)
                                .build())
                );
    }

    @Transactional
    public void unlikeImage(Long imageId, Long userId) {
        ArtistProfileImage image = EntityFinder.getOrThrow(imageRepository::findById, imageId, "이미지");
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");

        likeRepository.findByUserAndArtistProfileImage(user, image)
                .ifPresent(likeRepository::delete);
    }
}
