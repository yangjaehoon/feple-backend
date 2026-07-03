package com.feple.feple_backend.artist.photo.service;

import com.feple.feple_backend.artist.photo.entity.ArtistProfileImage;
import com.feple.feple_backend.artist.photo.entity.ArtistProfileImageLike;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.artist.photo.repository.ArtistProfileImageLikeRepository;
import com.feple.feple_backend.artist.photo.repository.ArtistProfileImageRepository;
import com.feple.feple_backend.global.EntityFinder;
import com.feple.feple_backend.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
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

        try {
            likeRepository.saveAndFlush(ArtistProfileImageLike.builder()
                    .user(user)
                    .artistProfileImage(image)
                    .build());
            imageRepository.incrementLikeCount(imageId);
        } catch (DataIntegrityViolationException ignored) {
            // 동시 요청으로 이미 좋아요됨
        }
    }

    @Transactional
    public void unlikeImage(Long imageId, Long userId) {
        int deleted = likeRepository.deleteByUserIdAndImageId(userId, imageId);
        if (deleted > 0) {
            imageRepository.decrementLikeCount(imageId);
        }
    }

    /** 회원 탈퇴 시 해당 유저의 아티스트 사진 좋아요 및 업로더 정보 일괄 제거 */
    @Transactional
    public void removeByUser(Long userId) {
        likeRepository.deleteByUserId(userId);
        imageRepository.nullifyUploaderByUserId(userId);
    }
}
