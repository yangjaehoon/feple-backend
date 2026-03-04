package com.feple.feple_backend.user.service;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.dto.comment.MyCommentResponseDto;
import com.feple.feple_backend.dto.post.PostResponseDto;
import com.feple.feple_backend.repository.CommentRepository;
import com.feple.feple_backend.repository.PostRepository;
import com.feple.feple_backend.user.domain.AuthProvider;
import com.feple.feple_backend.user.domain.User;
import com.feple.feple_backend.user.dto.KakaoUserResponse;
import com.feple.feple_backend.user.dto.OAuthUserInfo;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.dto.UserStatsDto;
import com.feple.feple_backend.user.repository.UserRepository;
//import jakarta.transaction.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ArtistFollowRepository artistFollowRepository;

    public User registerOrLogin(KakaoUserResponse kakaoUser) {
        // kakao_account 가 없으면 예외
        var account = Optional.ofNullable(kakaoUser.getKakaoAccount())
                .orElseThrow(() -> new IllegalArgumentException("카카오 계정 정보가 없습니다."));

        String oauthId = kakaoUser.getId().toString();
        //.orElseThrow(() -> new IllegalArgumentException("이메일 정보가 없습니다."));
        String email = account.getEmail();

        String nickname = Optional.ofNullable(account.getProfile())
                //.map(KakaoUserResponse.KakaoAccount.profile::getNickname)
                .map(KakaoUserResponse.Profile::getNickname)
                .filter(n -> !n.isBlank())
                .orElse("KakaoUser");

        return userRepository.findByOauthId(oauthId)
                .orElseGet(() -> userRepository.save(User.builder()
                        .oauthId(oauthId)
                        .email(email)
                        .nickname(nickname)
                        .provider(AuthProvider.KAKAO)
                        .profileImageUrl(account.getProfile().getProfile_image_url())
                        .build()
                ));
    }

    public Long createUser(OAuthUserInfo dto) {
        User user = User.builder()
                .email(dto.getEmail())
                .nickname(dto.getNickname())
                .oauthId(dto.getOauthId())
                .provider(dto.getProvider())
                .profileImageUrl(dto.getProfileImageUrl())
                .build();
        return userRepository.save(user).getId();
    }

    @Transactional(readOnly = true)
    public UserResponseDto getUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다. id=" + id));
        return UserResponseDto.from(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponseDto updateNickname(Long id, String nickname) {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("사용자를 찾을 수 없습니다. id=" + id)
                );
        user.changeNickname(nickname);
        return UserResponseDto.from(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<PostResponseDto> getMyPosts(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다. id=" + userId));
        return postRepository.findByUser(user).stream()
                .map(PostResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MyCommentResponseDto> getMyComments(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다. id=" + userId));
        return commentRepository.findByUser(user).stream()
                .map(MyCommentResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ArtistResponseDto> getFollowedArtists(Long userId) {
        return artistFollowRepository.findByUserId(userId).stream()
                .map(follow -> ArtistResponseDto.from(follow.getArtist()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserStatsDto getUserStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다. id=" + userId));
        long postCount = postRepository.countByUser(user);
        long commentCount = commentRepository.countByUser(user);
        return new UserStatsDto(postCount, commentCount);
    }

    public Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new IllegalStateException("Not authenticated");
        }
        String principal = auth.getPrincipal().toString();
        return Long.parseLong(principal);
    }


}
