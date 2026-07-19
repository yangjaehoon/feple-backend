package com.feple.feple_backend.post.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.badword.BadWordValidator;
import com.feple.feple_backend.certification.service.FestivalCertificationService;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.global.PageSize;
import com.feple.feple_backend.global.OwnershipValidator;
import com.feple.feple_backend.post.dto.CursorPage;
import com.feple.feple_backend.post.dto.CursorPageRequest;
import com.feple.feple_backend.post.dto.PostRequestDto;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import com.feple.feple_backend.post.event.PostCreatedEvent;
import com.feple.feple_backend.userblock.service.BlockedContentFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;
    private final FestivalRepository festivalRepository;
    private final FestivalCertificationService certificationService;
    private final BadWordValidator badWordFilter;
    private final ApplicationEventPublisher eventPublisher;
    private final PopularPostCache popularPostCache;
    private final BlockedContentFilter blockedContentFilter;

    private record PostContext(BoardType boardType, Artist artist, Festival festival) {}

    @Override
    @Transactional
    public Long createPost(PostRequestDto dto, Long userId) {
        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");
        Long postId = postRepository.save(buildPost(dto, user, new PostContext(dto.getBoardType(), null, null))).getId();
        eventPublisher.publishEvent(new PostCreatedEvent(userId, postId));
        return postId;
    }

    @Override
    public PostResponseDto getPost(Long postId) {
        Post post = EntityLoader.getOrThrow(postRepository::findWithAssociationsById, postId, "게시글");
        return PostResponseDto.from(post);
    }

    @Override
    public List<PostResponseDto> getPopularPosts(Long viewerId) {
        return blockedContentFilter.excludeBlocked(popularPostCache.getPopularPosts(), viewerId, PostResponseDto::getUserId);
    }

    @Override
    public CursorPage<PostResponseDto> getPostsByBoardTypeLatest(BoardType boardType, CursorPageRequest pageRequest) {
        Long cursor = pageRequest.cursor();
        return buildCursorPage(pageRequest,
                limit -> postRepository.findByBoardTypeOrderByIdDesc(boardType, limit),
                limit -> postRepository.findByBoardTypeAndIdLessThanOrderByIdDesc(boardType, cursor, limit),
                PostResponseDto::from);
    }

    @Override
    public CursorPage<PostResponseDto> getPostsByBoardTypePopular(BoardType boardType, CursorPageRequest pageRequest) {
        // likeCount는 동적으로 변하므로 offset 기반 유지, cursor는 페이지 번호를 opaque Long으로 전달
        Long cursor = pageRequest.cursor();
        int page = CursorPage.toPage(cursor);
        Page<Post> result = postRepository.findByBoardTypeOrderByLikeCountDescCreatedAtDesc(
                boardType, PageRequest.of(page, pageRequest.size()));
        List<PostResponseDto> content = blockedContentFilter.excludeBlocked(
                result.map(PostResponseDto::from).toList(), pageRequest.viewerId(), PostResponseDto::getUserId);
        return CursorPage.of(result, content, cursor);
    }

    @Override
    @Transactional
    public void updateOwnPost(Long postId, PostRequestDto dto, Long requestUserId) {
        Post post = EntityLoader.getOrThrow(postRepository::findById, postId, "게시글");
        OwnershipValidator.checkOwner(post.getUserId(), requestUserId, "게시글", "수정");
        validatePostContent(dto);
        post.update(dto.getTitle(), dto.getContent(), dto.getImageUrl());
    }

    @Override
    @Transactional
    public void deleteOwnPost(Long postId, Long requestUserId) {
        Post post = EntityLoader.getOrThrow(postRepository::findById, postId, "게시글");
        OwnershipValidator.checkOwner(post.getUserId(), requestUserId, "게시글");
        // soft delete: 행이 남아 FK 무결성 유지, 신고 등 증거 보존
        postRepository.deleteById(postId);
    }

    @Override
    public CursorPage<PostResponseDto> getPostsByArtistIdPaged(Long artistId, CursorPageRequest pageRequest) {
        Artist artist = EntityLoader.getOrThrow(artistRepository::findById, artistId, "아티스트");
        Long cursor = pageRequest.cursor();
        return buildCursorPage(pageRequest,
                limit -> postRepository.findByArtistOrderByIdDesc(artist, limit),
                limit -> postRepository.findByArtistAndIdLessThanOrderByIdDesc(artist, cursor, limit),
                PostResponseDto::from);
    }

    @Override
    @Transactional
    public Long createArtistPost(Long artistId, PostRequestDto dto, Long userId) {
        Artist artist = EntityLoader.getOrThrow(artistRepository::findById, artistId, "아티스트");
        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");
        Long postId = postRepository.save(buildPost(dto, user, new PostContext(null, artist, null))).getId();
        eventPublisher.publishEvent(new PostCreatedEvent(userId, postId));
        return postId;
    }

    @Override
    public CursorPage<PostResponseDto> getPostsByFestivalIdPaged(Long festivalId, CursorPageRequest pageRequest) {
        Festival festival = EntityLoader.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        Set<Long> certifiedUserIds = certificationService.findApprovedUserIdsByFestivalId(festivalId);
        Long cursor = pageRequest.cursor();
        return buildCursorPage(pageRequest,
                limit -> postRepository.findGeneralFestivalPostsOrderByIdDesc(festival, limit),
                limit -> postRepository.findGeneralFestivalPostsAndIdLessThanOrderByIdDesc(festival, cursor, limit),
                post -> PostResponseDto.from(post, certifiedUserIds.contains(post.getUserId())));
    }

    @Override
    @Transactional
    public Long createFestivalPost(Long festivalId, PostRequestDto dto, Long userId) {
        Festival festival = EntityLoader.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");
        Long postId = postRepository.save(buildPost(dto, user, new PostContext(null, null, festival))).getId();
        eventPublisher.publishEvent(new PostCreatedEvent(userId, postId));
        return postId;
    }

    @Override
    public CursorPage<PostResponseDto> getPostsByFestivalIdAndBoardTypePaged(Long festivalId, BoardType boardType, CursorPageRequest pageRequest) {
        Festival festival = EntityLoader.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        Set<Long> certifiedUserIds = certificationService.findApprovedUserIdsByFestivalId(festivalId);
        Long cursor = pageRequest.cursor();
        return buildCursorPage(pageRequest,
                limit -> postRepository.findByFestivalAndBoardTypeOrderByIdDesc(festival, boardType, limit),
                limit -> postRepository.findByFestivalAndBoardTypeAndIdLessThanOrderByIdDesc(festival, boardType, cursor, limit),
                post -> PostResponseDto.from(post, certifiedUserIds.contains(post.getUserId())));
    }

    @Override
    @Transactional
    public Long createFestivalTypedPost(Long festivalId, PostRequestDto dto, Long userId, BoardType boardType) {
        Festival festival = EntityLoader.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");
        Long postId = postRepository.save(buildPost(dto, user, new PostContext(boardType, null, festival))).getId();
        eventPublisher.publishEvent(new PostCreatedEvent(userId, postId));
        return postId;
    }

    @Override
    public List<PostResponseDto> getPopularFestivalPosts(Long festivalId, Long viewerId) {
        Festival festival = EntityLoader.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        Set<Long> certifiedUserIds = certificationService.findApprovedUserIdsByFestivalId(festivalId);
        List<PostResponseDto> posts = postRepository.findByFestivalOrderByLikeCountDesc(festival, PageRequest.of(0, PageSize.POSTS))
                .map(post -> PostResponseDto.from(post, certifiedUserIds.contains(post.getUserId())))
                .toList();
        return blockedContentFilter.excludeBlocked(posts, viewerId, PostResponseDto::getUserId);
    }

    @Override
    @Transactional
    public void incrementViewCount(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new NoSuchElementException("게시글을 찾을 수 없습니다: " + postId);
        }
        postRepository.incrementViewCount(postId);
    }

    // fetchFirst/fetchAfterCursor는 cursor==null 여부에 따라 호출부에서 다른 리포지토리 메서드를 넘긴다.
    // hasNext는 차단 필터링 전 fetch 개수(size+1) 기준으로 판단한다.
    private CursorPage<PostResponseDto> buildCursorPage(CursorPageRequest pageRequest,
                                                          Function<PageRequest, List<Post>> fetchFirst,
                                                          Function<PageRequest, List<Post>> fetchAfterCursor,
                                                          Function<Post, PostResponseDto> mapper) {
        int size = pageRequest.size();
        int fetchSize = size + 1;
        PageRequest limit = PageRequest.of(0, fetchSize);
        List<Post> posts = (pageRequest.cursor() == null) ? fetchFirst.apply(limit) : fetchAfterCursor.apply(limit);
        boolean hasNext = posts.size() == fetchSize;
        List<PostResponseDto> content = blockedContentFilter.excludeBlocked(
                posts.stream().limit(size).map(mapper).toList(), pageRequest.viewerId(), PostResponseDto::getUserId);
        Long nextCursor = hasNext && !content.isEmpty() ? content.get(content.size() - 1).getId() : null;
        return new CursorPage<>(content, nextCursor, hasNext);
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


    private void validatePostContent(PostRequestDto dto) {
        badWordFilter.validateField("title", dto.getTitle());
        badWordFilter.validateField("content", dto.getContent());
    }
}
