package com.feple.feple_backend.user.service;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.domain.post.Post;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.repository.FestivalLikeRepository;
import com.feple.feple_backend.dto.comment.MyCommentResponseDto;
import com.feple.feple_backend.dto.post.PostResponseDto;
import com.feple.feple_backend.repository.CommentRepository;
import com.feple.feple_backend.repository.PostLikeRepository;
import com.feple.feple_backend.repository.PostRepository;
import com.feple.feple_backend.user.domain.AuthProvider;
import com.feple.feple_backend.user.domain.User;
import com.feple.feple_backend.auth.dto.LocalLoginRequest;
import com.feple.feple_backend.auth.dto.RegisterRequest;
import com.feple.feple_backend.user.dto.KakaoUserResponse;
import com.feple.feple_backend.user.dto.OAuthUserInfo;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.dto.UserStatsDto;
import com.feple.feple_backend.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
//import jakarta.transaction.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

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
    private final PostLikeRepository postLikeRepository;
    private final ArtistFollowRepository artistFollowRepository;
    private final FestivalLikeRepository festivalLikeRepository;
    private final PasswordEncoder passwordEncoder;

    public User registerOrLogin(KakaoUserResponse kakaoUser) {
        // kakao_account 가 없으면 예외
        var account = Optional.ofNullable(kakaoUser.getKakaoAccount())
                .orElseThrow(() -> new IllegalArgumentException("카카오 계정 정보가 없습니다."));

        String oauthId = kakaoUser.getId().toString();
        // .orElseThrow(() -> new IllegalArgumentException("이메일 정보가 없습니다."));
        String email = account.getEmail();

        String nickname = Optional.ofNullable(account.getProfile())
                // .map(KakaoUserResponse.KakaoAccount.profile::getNickname)
                .map(KakaoUserResponse.Profile::getNickname)
                .filter(n -> !n.isBlank())
                .orElse("KakaoUser");

        Optional<User> existingUser = userRepository.findByOauthId(oauthId);
        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        User newUser = User.builder()
                .oauthId(oauthId)
                .email(email)
                .nickname(nickname)
                .provider(AuthProvider.KAKAO)
                .profileImageUrl(account.getProfile().getProfile_image_url())
                .build();

        @SuppressWarnings("null")
        User savedUser = userRepository.save(newUser);
        return savedUser;
    }

    @Transactional
    public User registerLocal(RegisterRequest req) {
        if (userRepository.findByProviderAndOauthId(AuthProvider.LOCAL, req.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
        String nickname = (req.getNickname() != null && !req.getNickname().isBlank())
                ? req.getNickname()
                : req.getEmail().split("@")[0];
        User user = User.builder()
                .email(req.getEmail())
                .nickname(nickname)
                .oauthId(req.getEmail())
                .provider(AuthProvider.LOCAL)
                .password(passwordEncoder.encode(req.getPassword()))
                .build();
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User loginLocal(LocalLoginRequest req) {
        User user = userRepository.findByProviderAndOauthId(AuthProvider.LOCAL, req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        return user;
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
    public UserResponseDto getUser(@NonNull Long id) {
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
    public UserResponseDto updateNickname(@NonNull Long id, String nickname) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다. id=" + id));
        user.changeNickname(nickname);
        return UserResponseDto.from(user);
    }

    public void deleteUser(@NonNull Long id) {
        userRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<UserResponseDto> getUsersPage(int page, int size, String keyword) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        if (keyword != null && !keyword.isBlank()) {
            return userRepository
                    .findByNicknameContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword, keyword, pageable)
                    .map(UserResponseDto::from);
        }
        return userRepository.findAll(pageable).map(UserResponseDto::from);
    }

    @Transactional
    public void bulkDeleteUsers(List<Long> ids) {
        for (Long id : ids) {
            adminDeleteUser(id);
        }
    }

    @Transactional
    public void adminDeleteUser(@NonNull Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다. id=" + id));

        // 1. 사용자의 댓글 삭제 (JPA 영속성 컨텍스트에서 REMOVED 상태로 전환)
        commentRepository.deleteAll(commentRepository.findByUser(user));

        // 2. 사용자 게시글의 좋아요 삭제
        List<Post> userPosts = postRepository.findByUser(user);
        for (Post post : userPosts) {
            postLikeRepository.deleteByPostId(post.getId());
        }

        // 3. 사용자가 누른 좋아요 삭제
        postLikeRepository.deleteByUser(user);

        // 4. 사용자 게시글 삭제 (cascade: 다른 사용자의 댓글도 삭제)
        postRepository.deleteAll(userPosts);

        // 5. 페스티벌 좋아요, 아티스트 팔로우 삭제
        festivalLikeRepository.deleteAll(festivalLikeRepository.findByUserId(id));
        artistFollowRepository.deleteAll(artistFollowRepository.findByUserId(id));

        // 6. 사용자 삭제
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public List<PostResponseDto> getMyPosts(@NonNull Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다. id=" + userId));
        return postRepository.findByUser(user).stream()
                .map(PostResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MyCommentResponseDto> getMyComments(@NonNull Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다. id=" + userId));
        return commentRepository.findByUser(user).stream()
                .map(MyCommentResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FestivalResponseDto> getLikedFestivals(@NonNull Long userId) {
        return festivalLikeRepository.findByUserId(userId).stream()
                .map(like -> FestivalResponseDto.from(like.getFestival()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ArtistResponseDto> getFollowedArtists(@NonNull Long userId) {
        return artistFollowRepository.findByUserId(userId).stream()
                .map(follow -> ArtistResponseDto.from(follow.getArtist()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserStatsDto getUserStats(@NonNull Long userId) {
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
