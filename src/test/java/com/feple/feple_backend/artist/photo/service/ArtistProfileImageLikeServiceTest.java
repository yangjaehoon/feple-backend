package com.feple.feple_backend.artist.photo.service;

import com.feple.feple_backend.artist.photo.entity.ArtistProfileImage;
import com.feple.feple_backend.artist.photo.repository.ArtistProfileImageLikeRepository;
import com.feple.feple_backend.artist.photo.repository.ArtistProfileImageRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ArtistProfileImageLikeServiceTest {

    @Mock ArtistProfileImageLikeRepository likeRepository;
    @Mock ArtistProfileImageRepository imageRepository;
    @Mock UserRepository userRepository;

    @InjectMocks ArtistProfileImageLikeService service;

    @Test
    void toggleLike_처음_좋아요면_저장_후_true() {
        ArtistProfileImage image = mock(ArtistProfileImage.class);
        User user = mock(User.class);
        given(imageRepository.findById(1L)).willReturn(Optional.of(image));
        given(userRepository.findById(2L)).willReturn(Optional.of(user));
        given(likeRepository.deleteByUserIdAndImageId(2L, 1L)).willReturn(0);

        boolean result = service.toggleLike(1L, 2L);

        assertThat(result).isTrue();
        then(likeRepository).should().saveAndFlush(any());
        then(imageRepository).should().incrementLikeCount(1L);
    }

    @Test
    void toggleLike_이미_좋아요했으면_삭제_후_false() {
        ArtistProfileImage image = mock(ArtistProfileImage.class);
        User user = mock(User.class);
        given(imageRepository.findById(1L)).willReturn(Optional.of(image));
        given(userRepository.findById(2L)).willReturn(Optional.of(user));
        given(likeRepository.deleteByUserIdAndImageId(2L, 1L)).willReturn(1);

        boolean result = service.toggleLike(1L, 2L);

        assertThat(result).isFalse();
        then(imageRepository).should().decrementLikeCount(1L);
        then(likeRepository).should(org.mockito.Mockito.never()).saveAndFlush(any());
    }

    @Test
    void toggleLike_동시_요청_경합시_true_반환() {
        ArtistProfileImage image = mock(ArtistProfileImage.class);
        User user = mock(User.class);
        given(imageRepository.findById(1L)).willReturn(Optional.of(image));
        given(userRepository.findById(2L)).willReturn(Optional.of(user));
        given(likeRepository.deleteByUserIdAndImageId(2L, 1L)).willReturn(0);
        willThrow(new DataIntegrityViolationException("dup"))
                .given(likeRepository).saveAndFlush(any());

        boolean result = service.toggleLike(1L, 2L);

        assertThat(result).isTrue();
        then(imageRepository).should(org.mockito.Mockito.never()).incrementLikeCount(any());
    }
}
