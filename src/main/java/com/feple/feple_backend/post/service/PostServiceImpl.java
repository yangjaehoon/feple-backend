package com.feple.feple_backend.post.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.certification.repository.FestivalCertificationRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.global.EntityFinder;
import com.feple.feple_backend.global.PageSize;
import com.feple.feple_backend.global.PermissionValidator;
import com.feple.feple_backend.post.dto.PostRequestDto;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.repository.PostLikeRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostServiceImpl implements PostService, PostAdminService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;
    private final FestivalRepository festivalRepository;
    private final FestivalCertificationRepository certificationRepository;

    @Override
    @Transactional
    public Long createPost(PostRequestDto dto, Long userId) {
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");
        return postRepository.save(buildPost(dto, user, dto.getBoardType(), null, null)).getId();
    }

    @Override
    public PostResponseDto getPost(Long postId) {
        Post post = EntityFinder.getOrThrow(postRepository::findById, postId, "게시글");
        return PostResponseDto.from(post);
    }

    @Override
    public List<PostResponseDto> getHotPosts() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        return postRepository.findHotPosts(oneWeekAgo, PageRequest.of(0, PageSize.HOT_POSTS)).stream()
                .map(PostResponseDto::from)
                .toList();
    }

    @Override
    public List<PostResponseDto> getPostsByBoardType(BoardType boardType) {
        return postRepository.findByBoardTypeOrderByCreatedAtDesc(boardType, PageRequest.of(0, PageSize.POSTS))
                .map(PostResponseDto::from)
                .toList();
    }

    @Override
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

    @Override
    public long getTotalPostCount() {
        return postRepository.count();
    }

    @Override
    public long countRecentPosts(int days) {
        return postRepository.countByCreatedAtAfter(LocalDateTime.now().minusDays(days));
    }

    @Override
    public List<PostResponseDto> getAdminHotPosts(int limit) {
        return postRepository.findHotPosts(LocalDateTime.now().minusWeeks(1), PageRequest.of(0, limit))
                .stream().map(PostResponseDto::from).toList();
    }

    @Override
    @Transactional
    public void deleteOwnPost(Long postId, Long requestUserId) {
        Post post = EntityFinder.getOrThrow(postRepository::findById, postId, "게시글");
        PermissionValidator.checkOwner(post.getUser().getId(), requestUserId, "게시글");
        postLikeRepository.deleteByPostId(postId);
        postRepository.deleteById(postId);
    }

    @Override
    @Transactional
    public void deletePost(Long postId) {
        postLikeRepository.deleteByPostId(postId);
        postRepository.deleteById(postId);
    }

    @Override
    @Transactional
    public void bulkDeletePosts(List<Long> ids) {
        for (Long id : ids) {
            deletePost(id);
        }
    }

    @Override
    public List<PostResponseDto> getPostsByArtistId(Long artistId) {
        Artist artist = EntityFinder.getOrThrow(artistRepository::findById, artistId, "아티스트");
        return postRepository.findByArtistOrderByCreatedAtDesc(artist, PageRequest.of(0, PageSize.POSTS))
                .map(PostResponseDto::from)
                .toList();
    }

    @Override
    @Transactional
    public Long createArtistPost(Long artistId, PostRequestDto dto, Long userId) {
        Artist artist = EntityFinder.getOrThrow(artistRepository::findById, artistId, "아티스트");
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");
        return postRepository.save(buildPost(dto, user, null, artist, null)).getId();
    }

    @Override
    public List<PostResponseDto> getPostsByFestivalId(Long festivalId) {
        Festival festival = EntityFinder.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        Set<Long> certifiedUserIds = certificationRepository.findApprovedUserIdsByFestivalId(festivalId);
        return postRepository.findByFestivalOrderByCreatedAtDesc(festival, PageRequest.of(0, PageSize.POSTS))
                .map(post -> PostResponseDto.from(post, certifiedUserIds.contains(post.getUser().getId())))
                .toList();
    }

    @Override
    @Transactional
    public Long createFestivalPost(Long festivalId, PostRequestDto dto, Long userId) {
        Festival festival = EntityFinder.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");
        return postRepository.save(buildPost(dto, user, null, null, festival)).getId();
    }

    @Override
    @Transactional
    public void deletePostsByUser(User user) {
        List<Post> posts = postRepository.findByUser(user);
        posts.forEach(post -> postLikeRepository.deleteByPostId(post.getId()));
        postLikeRepository.deleteByUser(user);
        postRepository.deleteAll(posts);
    }

    @Override
    @Transactional
    public void deletePostsByArtist(Artist artist) {
        List<Post> posts = postRepository.findByArtist(artist);
        posts.forEach(post -> postLikeRepository.deleteByPostId(post.getId()));
        postRepository.deleteAll(posts);
    }

    @Override
    @Transactional
    public void deletePostsByFestival(Festival festival) {
        List<Post> posts = postRepository.findByFestival(festival);
        for (Post post : posts) {
            postLikeRepository.deleteByPostId(post.getId());
        }
        postRepository.deleteAll(posts);
    }

    @Override
    public List<PostResponseDto> getMyPosts(Long userId) {
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");
        return postRepository.findByUserOrderByCreatedAtDesc(user, PageRequest.of(0, PageSize.MY_ACTIVITIES))
                .stream().map(PostResponseDto::from).toList();
    }

    @Override
    public long countMyPosts(Long userId) {
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");
        return postRepository.countByUser(user);
    }

    @Override
    public List<PostResponseDto> searchPosts(String keyword) {
        return postRepository.findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(
                        keyword, PageRequest.of(0, PageSize.SEARCH))
                .stream()
                .map(PostResponseDto::from)
                .toList();
    }

    private Optional<BoardType> parseBoardType(String filter) {
        if (filter == null) return Optional.empty();
        return switch (filter) {
            case "FREE" -> Optional.of(BoardType.FREE);
            case "MATE" -> Optional.of(BoardType.MATE);
            default     -> Optional.empty();
        };
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
