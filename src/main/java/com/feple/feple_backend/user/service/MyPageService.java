package com.feple.feple_backend.user.service;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.comment.dto.MyCommentResponseDto;
import com.feple.feple_backend.comment.repository.CommentRepository;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.repository.FestivalLikeRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.dto.UserStatsDto;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * 마이페이지 전용 조회 서비스.
 * 사용자 본인의 활동 데이터(게시글, 댓글, 좋아요, 팔로우)를 조회합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private static final int MY_PAGE_MAX = 200;

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final FestivalLikeRepository festivalLikeRepository;
    private final ArtistFollowRepository artistFollowRepository;
    private final FileStorageService fileStorageService;

    public List<PostResponseDto> getMyPosts(@NonNull Long userId) {
        User user = findUser(userId);
        return postRepository.findByUserOrderByCreatedAtDesc(user, PageRequest.of(0, MY_PAGE_MAX))
                .stream()
                .map(PostResponseDto::from)
                .toList();
    }

    public List<MyCommentResponseDto> getMyComments(@NonNull Long userId) {
        User user = findUser(userId);
        return commentRepository.findByUserOrderByCreatedAtDesc(user, PageRequest.of(0, MY_PAGE_MAX))
                .stream()
                .map(MyCommentResponseDto::from)
                .toList();
    }

    public List<FestivalResponseDto> getLikedFestivals(@NonNull Long userId) {
        return festivalLikeRepository.findByUserId(userId).stream()
                .map(like -> FestivalResponseDto.from(
                        like.getFestival(),
                        fileStorageService.buildUrl(like.getFestival().getPosterKey())))
                .collect(Collectors.toList());
    }

    public List<ArtistResponseDto> getFollowedArtists(@NonNull Long userId) {
        return artistFollowRepository.findByUserId(userId).stream()
                .map(follow -> ArtistResponseDto.from(
                        follow.getArtist(),
                        fileStorageService.buildUrl(follow.getArtist().getProfileImageKey())))
                .collect(Collectors.toList());
    }

    public UserStatsDto getUserStats(@NonNull Long userId) {
        User user = findUser(userId);
        long postCount = postRepository.countByUser(user);
        long commentCount = commentRepository.countByUser(user);
        return new UserStatsDto(postCount, commentCount);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다. id=" + userId));
    }
}
