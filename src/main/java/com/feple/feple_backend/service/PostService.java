package com.feple.feple_backend.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.domain.post.BoardType;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.domain.post.Post;
import com.feple.feple_backend.domain.post.PostLike;
import com.feple.feple_backend.user.domain.User;
import com.feple.feple_backend.dto.post.PostRequestDto;
import com.feple.feple_backend.dto.post.PostResponseDto;
import com.feple.feple_backend.repository.PostLikeRepository;
import com.feple.feple_backend.repository.PostRepository;
import com.feple.feple_backend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;
    private final FestivalRepository festivalRepository;

    @Transactional
    public Long createPost(PostRequestDto dto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("해당 사용자가 없습니다: " + userId));

        Post post = Post.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .boardType(dto.getBoardType())
                .likeCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .user(user)
                .build();

        return postRepository.save(post).getId();
    }

    public PostResponseDto getPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다: " + postId));
        return PostResponseDto.from(post);
    }

    public boolean isLikedByUser(Long postId, Long userId) {
        if (userId == null) return false;
        return postLikeRepository.existsByUserIdAndPostId(userId, postId);
    }

    public List<PostResponseDto> getHotPosts() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        return postRepository.findHotPosts(oneWeekAgo, PageRequest.of(0, 4)).stream()
                .map(PostResponseDto::from)
                .toList();
    }

    public List<PostResponseDto> getPostsByBoardType(BoardType boardType) {
        List<Post> posts = postRepository.findByBoardType(boardType);
        return posts.stream()
                .map(PostResponseDto::from)
                .toList();
    }

    @Transactional
    public boolean toggleLike(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다: " + postId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

        Optional<PostLike> existing = postLikeRepository.findByUserIdAndPostId(userId, postId);
        if (existing.isPresent()) {
            postLikeRepository.delete(existing.get());
            post.decrementLikeCount();
            return false; // 좋아요 취소
        } else {
            postLikeRepository.save(PostLike.builder().user(user).post(post).build());
            post.incrementLikeCount();
            return true; // 좋아요 추가
        }
    }

    public Page<PostResponseDto> getPostsForAdmin(int page, int size, String filter, String keyword) {
        PageRequest pageable = PageRequest.of(page, size);
        boolean hasKeyword = keyword != null && !keyword.isBlank();

        if ("FREE".equals(filter)) {
            return hasKeyword
                    ? postRepository.findByBoardTypeAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(BoardType.FREE, keyword, pageable).map(PostResponseDto::from)
                    : postRepository.findByBoardTypeOrderByCreatedAtDesc(BoardType.FREE, pageable).map(PostResponseDto::from);
        }
        if ("MATE".equals(filter)) {
            return hasKeyword
                    ? postRepository.findByBoardTypeAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(BoardType.MATE, keyword, pageable).map(PostResponseDto::from)
                    : postRepository.findByBoardTypeOrderByCreatedAtDesc(BoardType.MATE, pageable).map(PostResponseDto::from);
        }
        return hasKeyword
                ? postRepository.findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(keyword, pageable).map(PostResponseDto::from)
                : postRepository.findAllByOrderByCreatedAtDesc(pageable).map(PostResponseDto::from);
    }

    public long getTotalPostCount() {
        return postRepository.count();
    }

    public long countRecentPosts(int days) {
        return postRepository.countByCreatedAtAfter(LocalDateTime.now().minusDays(days));
    }

    public List<PostResponseDto> getAdminHotPosts(int limit) {
        return postRepository.findHotPosts(LocalDateTime.now().minusWeeks(1), PageRequest.of(0, limit))
                .stream().map(PostResponseDto::from).toList();
    }

    @Transactional
    public void deletePost(Long postId) {
        postLikeRepository.deleteByPostId(postId);
        postRepository.deleteById(postId);
    }

    @Transactional
    public void bulkDeletePosts(List<Long> ids) {
        for (Long id : ids) {
            deletePost(id);
        }
    }

    public List<PostResponseDto> getPostsByArtistId(Long artistId) {
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new RuntimeException("해당 아티스트가 없습니다: " + artistId));
        List<Post> posts = postRepository.findByArtist(artist);
        return posts.stream()
                .map(PostResponseDto::from)
                .toList();
    }

    @Transactional
    public Long createArtistPost(Long artistId, PostRequestDto dto, Long userId) {
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new RuntimeException("해당 아티스트가 없습니다: " + artistId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("해당 사용자가 없습니다: " + userId));

        Post post = Post.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .boardType(null)
                .likeCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .user(user)
                .artist(artist)
                .build();

        return postRepository.save(post).getId();
    }

    public List<PostResponseDto> getPostsByFestivalId(Long festivalId) {
        Festival festival = festivalRepository.findById(festivalId)
                .orElseThrow(() -> new RuntimeException("해당 페스티벌이 없습니다: " + festivalId));
        List<Post> posts = postRepository.findByFestival(festival);
        return posts.stream()
                .map(PostResponseDto::from)
                .toList();
    }

    @Transactional
    public Long createFestivalPost(Long festivalId, PostRequestDto dto, Long userId) {
        Festival festival = festivalRepository.findById(festivalId)
                .orElseThrow(() -> new RuntimeException("해당 페스티벌이 없습니다: " + festivalId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("해당 사용자가 없습니다: " + userId));

        Post post = Post.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .boardType(null)
                .likeCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .user(user)
                .festival(festival)
                .build();

        return postRepository.save(post).getId();
    }
}
