package com.feple.feple_backend.post.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.badword.BadWordFilter;
import com.feple.feple_backend.certification.repository.FestivalCertificationRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.global.CountRowMapper;
import com.feple.feple_backend.global.EntityFinder;
import com.feple.feple_backend.global.LikeEscaper;
import com.feple.feple_backend.global.PageSize;
import com.feple.feple_backend.global.PermissionValidator;
import com.feple.feple_backend.post.dto.CursorPage;
import com.feple.feple_backend.post.dto.PostAdminFilterDto;
import com.feple.feple_backend.post.dto.PostRequestDto;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.comment.service.CommentService;
import com.feple.feple_backend.post.repository.PostLikeRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.notification.repository.NotificationRepository;
import com.feple.feple_backend.post.repository.PostReportRepository;
import com.feple.feple_backend.post.repository.PostScrapRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import com.feple.feple_backend.post.event.PostDeletedByAdminEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostServiceImpl implements PostService, PostAdminService, PostCascadeService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostScrapRepository postScrapRepository;
    private final PostReportRepository postReportRepository;
    private final CommentService commentService;
    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;
    private final FestivalRepository festivalRepository;
    private final FestivalCertificationRepository certificationRepository;
    private final NotificationRepository notificationRepository;
    private final BadWordFilter badWordFilter;
    private final ApplicationEventPublisher eventPublisher;

    private record PostContext(BoardType boardType, Artist artist, Festival festival) {}

    @Override
    @Transactional
    public Long createPost(PostRequestDto dto, Long userId) {
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");
        return postRepository.save(buildPost(dto, user, new PostContext(dto.getBoardType(), null, null))).getId();
    }

    @Override
    public PostResponseDto getPost(Long postId) {
        Post post = EntityFinder.getOrThrow(postRepository::findWithAssociationsById, postId, "게시글");
        return PostResponseDto.from(post);
    }

    @Override
    @Cacheable("hotPosts")
    public List<PostResponseDto> getHotPosts() {
        return postRepository.findHotPosts(oneWeekAgo(), PageRequest.of(0, PageSize.HOT_POSTS)).stream()
                .map(PostResponseDto::from)
                .toList();
    }

    @Override
    public List<PostResponseDto> getPostsByBoardType(BoardType boardType) {
        return getPostsByBoardTypePaged(boardType, null, PageSize.POSTS).content();
    }

    @Override
    public CursorPage<PostResponseDto> getPostsByBoardTypePaged(BoardType boardType, Long cursor, int size) {
        int fetchSize = size + 1;
        PageRequest limit = PageRequest.of(0, fetchSize);
        List<Post> posts = (cursor == null)
                ? postRepository.findByBoardTypeOrderByIdDesc(boardType, limit)
                : postRepository.findByBoardTypeAndIdLessThanOrderByIdDesc(boardType, cursor, limit);
        boolean hasNext = posts.size() == fetchSize;
        List<PostResponseDto> content = posts.stream().limit(size).map(PostResponseDto::from).toList();
        Long nextCursor = hasNext ? content.get(content.size() - 1).getId() : null;
        return new CursorPage<>(content, nextCursor, hasNext);
    }

    @Override
    public CursorPage<PostResponseDto> getPostsByBoardTypePopular(BoardType boardType, Long cursor, int size) {
        // likeCount는 동적으로 변하므로 offset 기반 유지, cursor는 페이지 번호를 opaque Long으로 전달
        int page = toPage(cursor);
        Page<Post> result = postRepository.findByBoardTypeOrderByLikeCountDescCreatedAtDesc(
                boardType, PageRequest.of(page, size));
        List<PostResponseDto> content = result.map(PostResponseDto::from).toList();
        return toCursorPage(result, content, cursor);
    }

    @Override
    public Page<PostResponseDto> getPostsForAdmin(PostAdminFilterDto params) {
        PageRequest pageable = PageRequest.of(params.page(), params.size());
        boolean hasKeyword = params.keyword() != null && !params.keyword().isBlank();
        String kw = LikeEscaper.escapeOrEmpty(params.keyword());

        // BoardType enum에 해당하는 필터(FREE, MATE, FESTIVAL_COMPANION 등)는 enum이 직접 처리.
        // 새 BoardType을 추가해도 이 메서드를 수정할 필요 없음.
        return BoardType.fromAdminFilter(params.filter())
                .map(boardType -> hasKeyword
                        ? postRepository.findByBoardTypeAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(boardType, kw, pageable)
                        : postRepository.findByBoardTypeOrderByCreatedAtDesc(boardType, pageable))
                .orElseGet(() -> resolveRelationFilter(params.filter(), params.artistId(), params.festivalId(), hasKeyword, kw, pageable))
                .map(PostResponseDto::from);
    }

    private Page<Post> resolveRelationFilter(String filter, Long artistId, Long festivalId,
                                             boolean hasKeyword, String kw, PageRequest pageable) {
        return switch (filter == null ? "" : filter) {
            case "ARTIST" -> artistId != null
                    ? (hasKeyword
                        ? postRepository.findByArtistIdAndTitleLikeOrderByCreatedAtDesc(artistId, kw, pageable)
                        : postRepository.findByArtistIdOrderByCreatedAtDesc(artistId, pageable))
                    : (hasKeyword
                        ? postRepository.findByArtistIsNotNullAndTitleLikeOrderByCreatedAtDesc(kw, pageable)
                        : postRepository.findByArtistIsNotNullOrderByCreatedAtDesc(pageable));
            case "FESTIVAL" -> festivalId != null
                    ? (hasKeyword
                        ? postRepository.findByFestivalIdAndTitleLikeOrderByCreatedAtDesc(festivalId, kw, pageable)
                        : postRepository.findByFestivalIdOrderByCreatedAtDesc(festivalId, pageable))
                    : (hasKeyword
                        ? postRepository.findByFestivalIsNotNullAndTitleLikeOrderByCreatedAtDesc(kw, pageable)
                        : postRepository.findByFestivalIsNotNullOrderByCreatedAtDesc(pageable));
            default -> hasKeyword
                    ? postRepository.findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(kw, pageable)
                    : postRepository.findAllByOrderByCreatedAtDesc(pageable);
        };
    }

    @Override
    @Cacheable(value = "adminDashboardStats", key = "'totalPosts'")
    public long getTotalPostCount() {
        return postRepository.count();
    }

    @Override
    @Cacheable(value = "adminDashboardStats", key = "'recentPosts_' + #days")
    public long countRecentPosts(int days) {
        return postRepository.countByCreatedAtAfter(LocalDateTime.now().minusDays(days));
    }

    @Override
    @Cacheable(value = "adminDashboardStats", key = "'adminHotPosts_' + #limit")
    public List<PostResponseDto> getAdminHotPosts(int limit) {
        return postRepository.findHotPosts(oneWeekAgo(), PageRequest.of(0, limit))
                .stream().map(PostResponseDto::from).toList();
    }

    @Override
    @Transactional
    public void updateOwnPost(Long postId, PostRequestDto dto, Long requestUserId) {
        Post post = EntityFinder.getOrThrow(postRepository::findById, postId, "게시글");
        PermissionValidator.checkOwner(post.getUserId(), requestUserId, "게시글");
        validatePostContent(dto);
        post.update(dto.getTitle(), dto.getContent(), dto.getImageUrl());
    }

    @Override
    @Transactional
    public void deleteOwnPost(Long postId, Long requestUserId) {
        Post post = EntityFinder.getOrThrow(postRepository::findById, postId, "게시글");
        PermissionValidator.checkOwner(post.getUserId(), requestUserId, "게시글");
        // soft delete: 행이 남아 FK 무결성 유지, 신고 등 증거 보존
        postRepository.deleteById(postId);
    }

    @Override
    @Transactional
    public void deletePost(Long postId) {
        Post post = EntityFinder.getOrThrow(postRepository::findById, postId, "게시글");
        eventPublisher.publishEvent(new PostDeletedByAdminEvent(post.getUserId(), post.getTitle()));
        postRepository.deleteById(postId);
    }

    @Override
    @Transactional
    public void bulkDeletePosts(List<Long> ids) {
        if (ids.isEmpty()) return;
        postRepository.softDeleteByIds(ids);
    }

    @Override
    @Transactional(readOnly = true)
    public long countPostsContaining(String word) {
        return postRepository.countByTitleOrContentContaining(LikeEscaper.escape(word.toLowerCase()));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> getPostCountsByUserIds(List<Long> userIds) {
        if (userIds.isEmpty()) return Map.of();
        return CountRowMapper.toLongMap(postRepository.countGroupByUserId(userIds));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostResponseDto> getDeletedPosts(int limit) {
        return postRepository.findSoftDeleted(limit).stream()
                .map(PostResponseDto::from)
                .toList();
    }

    @Override
    @Transactional
    public void restorePost(Long postId) {
        postRepository.restore(postId);
    }

    @Override
    public List<PostResponseDto> getPostsByArtistId(Long artistId) {
        Artist artist = EntityFinder.getOrThrow(artistRepository::findById, artistId, "아티스트");
        return postRepository.findByArtistOrderByCreatedAtDesc(artist, PageRequest.of(0, PageSize.POSTS))
                .map(PostResponseDto::from)
                .toList();
    }

    @Override
    public CursorPage<PostResponseDto> getPostsByArtistIdPaged(Long artistId, Long cursor, int size) {
        Artist artist = EntityFinder.getOrThrow(artistRepository::findById, artistId, "아티스트");
        int page = toPage(cursor);
        Page<Post> result = postRepository.findByArtistOrderByCreatedAtDesc(artist, PageRequest.of(page, size));
        List<PostResponseDto> content = result.getContent().stream().map(PostResponseDto::from).toList();
        return toCursorPage(result, content, cursor);
    }

    @Override
    @Transactional
    public Long createArtistPost(Long artistId, PostRequestDto dto, Long userId) {
        Artist artist = EntityFinder.getOrThrow(artistRepository::findById, artistId, "아티스트");
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");
        return postRepository.save(buildPost(dto, user, new PostContext(null, artist, null))).getId();
    }

    @Override
    public List<PostResponseDto> getPostsByFestivalId(Long festivalId) {
        Festival festival = EntityFinder.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        Set<Long> certifiedUserIds = certificationRepository.findApprovedUserIdsByFestivalId(festivalId);
        return postRepository.findGeneralFestivalPosts(festival, PageRequest.of(0, PageSize.POSTS))
                .map(post -> PostResponseDto.from(post, certifiedUserIds.contains(post.getUserId())))
                .toList();
    }

    @Override
    public CursorPage<PostResponseDto> getPostsByFestivalIdPaged(Long festivalId, Long cursor, int size) {
        Festival festival = EntityFinder.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        Set<Long> certifiedUserIds = certificationRepository.findApprovedUserIdsByFestivalId(festivalId);
        int page = toPage(cursor);
        Page<Post> result = postRepository.findGeneralFestivalPosts(festival, PageRequest.of(page, size));
        List<PostResponseDto> content = result.getContent().stream()
                .map(post -> PostResponseDto.from(post, certifiedUserIds.contains(post.getUserId())))
                .toList();
        return toCursorPage(result, content, cursor);
    }

    @Override
    @Transactional
    public Long createFestivalPost(Long festivalId, PostRequestDto dto, Long userId) {
        Festival festival = EntityFinder.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");
        return postRepository.save(buildPost(dto, user, new PostContext(null, null, festival))).getId();
    }

    @Override
    public List<PostResponseDto> getPostsByFestivalIdAndBoardType(Long festivalId, BoardType boardType) {
        Festival festival = EntityFinder.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        Set<Long> certifiedUserIds = certificationRepository.findApprovedUserIdsByFestivalId(festivalId);
        return postRepository.findByFestivalAndBoardTypeOrderByCreatedAtDesc(festival, boardType, PageRequest.of(0, PageSize.POSTS))
                .map(post -> PostResponseDto.from(post, certifiedUserIds.contains(post.getUserId())))
                .toList();
    }

    @Override
    public CursorPage<PostResponseDto> getPostsByFestivalIdAndBoardTypePaged(Long festivalId, BoardType boardType, Long cursor, int size) {
        Festival festival = EntityFinder.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        Set<Long> certifiedUserIds = certificationRepository.findApprovedUserIdsByFestivalId(festivalId);
        int page = toPage(cursor);
        Page<Post> result = postRepository.findByFestivalAndBoardTypeOrderByCreatedAtDesc(festival, boardType, PageRequest.of(page, size));
        List<PostResponseDto> content = result.getContent().stream()
                .map(post -> PostResponseDto.from(post, certifiedUserIds.contains(post.getUserId())))
                .toList();
        return toCursorPage(result, content, cursor);
    }

    @Override
    @Transactional
    public Long createFestivalTypedPost(Long festivalId, PostRequestDto dto, Long userId, BoardType boardType) {
        Festival festival = EntityFinder.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");
        return postRepository.save(buildPost(dto, user, new PostContext(boardType, null, festival))).getId();
    }

    @Override
    public List<PostResponseDto> getPopularFestivalPosts(Long festivalId) {
        Festival festival = EntityFinder.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        Set<Long> certifiedUserIds = certificationRepository.findApprovedUserIdsByFestivalId(festivalId);
        return postRepository.findByFestivalOrderByLikeCountDesc(festival, PageRequest.of(0, PageSize.POSTS))
                .map(post -> PostResponseDto.from(post, certifiedUserIds.contains(post.getUserId())))
                .toList();
    }

    @Override
    @Transactional
    public void deletePostsByArtist(Artist artist) {
        postRepository.nullifyArtistIdForSoftDeleted(artist.getId());
        deletePostLikesAndPosts(postRepository.findByArtist(artist));
    }

    @Override
    @Transactional
    public void deletePostsByFestival(Festival festival) {
        postRepository.nullifyFestivalIdForSoftDeleted(festival.getId());
        deletePostLikesAndPosts(postRepository.findByFestival(festival));
    }

    @Override
    @Transactional
    public void removePostActivityByUser(Long userId) {
        postLikeRepository.decrementPostLikeCountByUserId(userId);
        postLikeRepository.deleteByUserId(userId);
        postScrapRepository.decrementPostScrapCountByUserId(userId);
        postScrapRepository.deleteByUserId(userId);
    }

    @Override
    public List<PostResponseDto> getMyPosts(Long userId) {
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");
        return postRepository.findByUserOrderByCreatedAtDesc(user, PageRequest.of(0, PageSize.MY_ACTIVITIES))
                .stream().map(PostResponseDto::from).toList();
    }

    @Override
    public CursorPage<PostResponseDto> getMyPostsPaged(Long userId, Long cursor, int size) {
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");
        int page = toPage(cursor);
        Page<Post> result = postRepository.findByUserOrderByCreatedAtDesc(user, PageRequest.of(page, size));
        List<PostResponseDto> content = result.getContent().stream().map(PostResponseDto::from).toList();
        return toCursorPage(result, content, cursor);
    }

    @Override
    public CursorPage<PostResponseDto> getPublicPostsPaged(Long userId, Long cursor, int size) {
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");
        int page = toPage(cursor);
        Page<Post> result = postRepository.findPublicByUserOrderByCreatedAtDesc(user, PageRequest.of(page, size));
        List<PostResponseDto> content = result.getContent().stream().map(PostResponseDto::from).toList();
        return toCursorPage(result, content, cursor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostResponseDto> getRecentPostsByUser(Long userId, int limit) {
        return postRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit))
                .stream().map(PostResponseDto::from).toList();
    }

    @Override
    public long countMyPosts(Long userId) {
        return postRepository.countByUserId(userId);
    }

    @Override
    @Transactional
    public void incrementViewCount(Long postId) {
        Post post = EntityFinder.getOrThrow(postRepository::findById, postId, "게시글");
        post.incrementViewCount();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostResponseDto> getLikedPosts(Long userId) {
        return postLikeRepository.findPostsByUserId(userId, PageRequest.of(0, PageSize.MY_ACTIVITIES))
                .stream()
                .map(PostResponseDto::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countLikedPosts(Long userId) {
        return postLikeRepository.countByUserId(userId);
    }

    @Override
    public List<PostResponseDto> searchPosts(String keyword, String boardType) {
        String escaped = LikeEscaper.escape(keyword.trim());
        Optional<BoardType> type = parseBoardType(boardType);
        if (type.isPresent()) {
            return postRepository.findByBoardTypeAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(
                            type.get(), escaped, PageRequest.of(0, PageSize.SEARCH))
                    .stream()
                    .map(PostResponseDto::from)
                    .toList();
        }
        return postRepository.findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(
                        escaped, PageRequest.of(0, PageSize.SEARCH))
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

    private Post buildPost(PostRequestDto dto, User user, PostContext ctx) {
        validatePostContent(dto);
        return Post.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .boardType(ctx.boardType())
                .likeCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .user(user)
                .artist(ctx.artist())
                .festival(ctx.festival())
                .anonymous(dto.isAnonymous())
                .imageUrl(dto.getImageUrl())
                .build();
    }

    private void deletePostLikesAndPosts(List<Post> posts) {
        if (posts.isEmpty()) return;
        List<Long> postIds = posts.stream().map(Post::getId).toList();
        commentService.deleteByPostIds(postIds);
        postLikeRepository.deleteByPostIds(postIds);
        postScrapRepository.deleteByPostIds(postIds);
        postReportRepository.deleteByPostIds(postIds);
        notificationRepository.deleteByPostIdIn(postIds);
        postRepository.deleteAllByIdInBatch(postIds);
    }

    private LocalDateTime oneWeekAgo() {
        return LocalDateTime.now().minusWeeks(1);
    }

    private void validatePostContent(PostRequestDto dto) {
        badWordFilter.validateField("title", dto.getTitle());
        badWordFilter.validateField("content", dto.getContent());
    }

    private static int toPage(Long cursor) {
        return cursor == null ? 0 : cursor.intValue();
    }

    private static <T> CursorPage<T> toCursorPage(Page<?> result, List<T> content, Long cursor) {
        boolean hasNext = result.hasNext();
        Long nextCursor = hasNext ? (long)(toPage(cursor) + 1) : null;
        return new CursorPage<>(content, nextCursor, hasNext);
    }
}
