package com.feple.feple_backend.artist.photo.service;

import com.feple.feple_backend.artist.photo.entity.ArtistProfileImage;
import com.feple.feple_backend.artist.photo.entity.ArtistProfileImageLike;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.artist.photo.repository.ArtistProfileImageLikeRepository;
import com.feple.feple_backend.artist.photo.repository.ArtistProfileImageRepository;
import com.feple.feple_backend.user.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ArtistProfileImageLikeService {
    private final ArtistProfileImageLikeRepository likeRepository;
    private final ArtistProfileImageRepository imageRepository;
    private final UserRepository userRepository;

    @Transactional
    public void likeImage(Long imageId, Long userId){
        ArtistProfileImage image = imageRepository.findById(imageId)
                .orElseThrow(()-> new NoSuchElementException("이미지를 찾을 수 없습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new NoSuchElementException("사용자를 찾을 수 없습니다."));

        likeRepository.findByUserAndArtistProfileImage(user, image)
                .ifPresentOrElse(
                        like -> {},
                        ()-> {
                            ArtistProfileImageLike like = ArtistProfileImageLike.builder()
                                    .user(user)
                                    .artistProfileImage(image)
                                    .build();
                            likeRepository.save(like);
                        }
                );
    }

    @Transactional
    public void unlikeImage(Long imageId, Long userId) {
        ArtistProfileImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new NoSuchElementException("이미지를 찾을 수 없습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));

        likeRepository.findByUserAndArtistProfileImage(user, image)
                .ifPresent(likeRepository::delete);
    }
}
