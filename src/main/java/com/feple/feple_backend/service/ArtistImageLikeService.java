package com.feple.feple_backend.service;

import com.feple.feple_backend.domain.artist.ArtistImage;
import com.feple.feple_backend.domain.artist.ArtistImageLike;
import com.feple.feple_backend.domain.user.User;
import com.feple.feple_backend.repository.ArtistImageLikeRepository;
import com.feple.feple_backend.repository.ArtistImageRepository;
import com.feple.feple_backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArtistImageLikeService {
    private final ArtistImageLikeRepository likeRepository;
    private final ArtistImageRepository imageRepository;
    private final UserRepository userRepository;

    @Transactional
    public void likeImage(Long imageId, Long userId){
        ArtistImage image = imageRepository.findById(imageId)
                .orElseThrow(()-> new IllegalArgumentException("이미지를 찾을 수 없습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        likeRepository.findByUserAndArtistImage(user, image)
                .ifPresentOrElse(
                        like -> {},
                        ()-> {
                            ArtistImageLike like = ArtistImageLike.builder()
                                    .user(user)
                                    .artistImage(image)
                                    .build();
                            likeRepository.save(like);
                        }
                );
    }

    @Transactional
    public void unlikeImage(Long imageId, Long userId) {
        ArtistImage image = imageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("이미지를 찾을 수 없습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        likeRepository.findByUserAndArtistImage(user, image)
                .ifPresent(likeRepository::delete);
    }
}
