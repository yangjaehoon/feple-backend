package com.feple.feple_backend.post.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.certification.repository.FestivalCertificationRepository;
import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.post.dto.PostRequestDto;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.repository.PostLikeRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;
    private final FestivalRepository festivalRepository;
    private final FestivalCertificationRepository certificationRepository;

    @Transactional
    public Long createPost(PostRequestDto dto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("해당 사용자가 없습니다: " + userId));

        return postRepository.save(buildPost(dto, user, dto.getBoardType(), null, null)).getId();
    }

    public PostResponseDto getPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다: " + postId));
        return PostResponseDto.from(post);
    }

    public List<PostResponseDto> getHotPosts() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        return postRepository.findHotPosts(oneWeekAgo, PageRequest.of(0, 4)).stream()
                .map(PostResponseDto::from)
                .toList();
    }

    public List<PostResponseDto> getPostsByBoardType(BoardType boardType) {
        return postRepository.findByBoardTypeOrderByCreatedAtDesc(boardType, PageRequest.of(0, 100))
                .map(PostResponseDto::from)
                .toList();
    }

    public Page<PostResponseDto> getPostsForAdmin(int page, int size, String filter, String keyword) {
        PageRequest pageable = PageRequest.of(page, size);
        Optional<BoardType> boardType = parseBoardType(filter);
        boolean hasKeyword = keyword != null && !keyword.isBlank();

        Page<Post> result = boardType.map(bt ->
            hasKeyword
                ? postRepository.findByBoardTypeAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(bt, keyword, pageable)
                : postRepository.findByBoardTypeOrderByCreatedAtDesc(bt, pageable)
        ).orElseGet(() ->
            hasKeyword
                ? postRepository.findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(keyword, pageable)
                : postRepository.findAllByOrderByCreatedAtDesc(pageable)
        );
        return result.map(PostResponseDto::from);
    }

    private Optional<BoardType> parseBoardType(String filter) {
        if (filter == null) return Optional.empty();
        return switch (filter) {
            case "FREE" -> Optional.of(BoardType.FREE);
            case "MATE" -> Optional.of(BoardType.MATE);
            default     -> Optional.empty();
        };
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

    /** 일반 유저용 - 본인 게시글만 삭제 가능 */
    @Transactional
    public void deleteOwnPost(Long postId, Long requestUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다: " + postId));
        if (!post.getUser().getId().equals(requestUserId)) {
            throw new AccessDeniedException("본인의 게시글만 삭제할 수 있습니다.");
        }
        postLikeRepository.deleteByPostId(postId);
        postRepository.deleteById(postId);
    }

    /** 관리자용 - 소유권 검사 없이 삭제 */
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
                .orElseThrow(() -> new NoSuchElementException("해당 아티스트가 없습니다: " + artistId));
        return postRepository.findByArtistOrderByCreatedAtDesc(artist, PageRequest.of(0, 100))
                .map(PostResponseDto::from)
                .toList();
    }

    @Transactional
    public Long createArtistPost(Long artistId, PostRequestDto dto, Long userId) {
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new NoSuchElementException("해당 아티스트가 없습니다: " + artistId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("해당 사용자가 없습니다: " + userId));

        return postRepository.save(buildPost(dto, user, null, artist, null)).getId();
    }

    public List<PostResponseDto> getPostsByFestivalId(Long festivalId) {
        Festival festival = festivalRepository.findById(festivalId)
                .orElseThrow(() -> new NoSuchElementException("해당 페스티벌이 없습니다: " + festivalId));
        Set<Long> certifiedUserIds = certificationRepository.findApprovedUserIdsByFestivalId(festivalId);
        return postRepository.findByFestivalOrderByCreatedAtDesc(festival, PageRequest.of(0, 100))
                .map(post -> PostResponseDto.from(post, certifiedUserIds.contains(post.getUser().getId())))
                .toList();
    }

    @Transactional
    public Long createFestivalPost(Long festivalId, PostRequestDto dto, Long userId) {
        Festival festival = festivalRepository.findById(festivalId)
                .orElseThrow(() -> new NoSuchElementException("해당 페스티벌이 없습니다: " + festivalId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("해당 사용자가 없습니다: " + userId));

        return postRepository.save(buildPost(dto, user, null, null, festival)).getId();
    }

    private Post buildPost(PostRequestDto dto, User user, BoardType boardType, Artist artist, Festival festival) {
        return Post.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .boardType(boardType)
                .likeCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .user(user)
                .artist(artist)
                .festival(festival)
                .build();
    }
}
