package com.feple.feple_backend.post.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.badword.BadWordFilter;
import com.feple.feple_backend.certification.service.FestivalCertificationService;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.global.EntityFinder;
import com.feple.feple_backend.global.PageSize;
import com.feple.feple_backend.global.PermissionValidator;
import com.feple.feple_backend.post.dto.CursorPage;
import com.feple.feple_backend.post.dto.PostRequestDto;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import com.feple.feple_backend.post.event.PostCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;
    private final FestivalRepository festivalRepository;
    private final FestivalCertificationService certificationService;
    private final BadWordFilter badWordFilter;
    private final ApplicationEventPublisher eventPublisher;

    private record PostContext(BoardType boardType, Artist artist, Festival festival) {}

    @Override
    @Transactional
    public Long createPost(PostRequestDto dto, Long userId) {
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");
        Long postId = postRepository.save(buildPost(dto, user, new PostContext(dto.getBoardType(), null, null))).getId();
        eventPublisher.publishEvent(new PostCreatedEvent(userId, postId));
        return postId;
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
        int page = CursorPage.toPage(cursor);
        Page<Post> result = postRepository.findByBoardTypeOrderByLikeCountDescCreatedAtDesc(
                boardType, PageRequest.of(page, size));
        List<PostResponseDto> content = result.map(PostResponseDto::from).toList();
        return CursorPage.of(result, content, cursor);
    }

    @Override
    @Transactional
    public void updateOwnPost(Long postId, PostRequestDto dto, Long requestUserId) {
        Post post = EntityFinder.getOrThrow(postRepository::findById, postId, "게시글");
        PermissionValidator.checkOwner(post.getUserId(), requestUserId, "게시글", "수정");
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
    public CursorPage<PostResponseDto> getPostsByArtistIdPaged(Long artistId, Long cursor, int size) {
        Artist artist = EntityFinder.getOrThrow(artistRepository::findById, artistId, "아티스트");
        int page = CursorPage.toPage(cursor);
        Page<Post> result = postRepository.findByArtistOrderByCreatedAtDesc(artist, PageRequest.of(page, size));
        List<PostResponseDto> content = result.getContent().stream().map(PostResponseDto::from).toList();
        return CursorPage.of(result, content, cursor);
    }

    @Override
    @Transactional
    public Long createArtistPost(Long artistId, PostRequestDto dto, Long userId) {
        Artist artist = EntityFinder.getOrThrow(artistRepository::findById, artistId, "아티스트");
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");
        Long postId = postRepository.save(buildPost(dto, user, new PostContext(null, artist, null))).getId();
        eventPublisher.publishEvent(new PostCreatedEvent(userId, postId));
        return postId;
    }

    @Override
    public CursorPage<PostResponseDto> getPostsByFestivalIdPaged(Long festivalId, Long cursor, int size) {
        Festival festival = EntityFinder.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        Set<Long> certifiedUserIds = certificationService.findApprovedUserIdsByFestivalId(festivalId);
        int page = CursorPage.toPage(cursor);
        Page<Post> result = postRepository.findGeneralFestivalPosts(festival, PageRequest.of(page, size));
        List<PostResponseDto> content = result.getContent().stream()
                .map(post -> PostResponseDto.from(post, certifiedUserIds.contains(post.getUserId())))
                .toList();
        return CursorPage.of(result, content, cursor);
    }

    @Override
    @Transactional
    public Long createFestivalPost(Long festivalId, PostRequestDto dto, Long userId) {
        Festival festival = EntityFinder.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");
        Long postId = postRepository.save(buildPost(dto, user, new PostContext(null, null, festival))).getId();
        eventPublisher.publishEvent(new PostCreatedEvent(userId, postId));
        return postId;
    }

    @Override
    public CursorPage<PostResponseDto> getPostsByFestivalIdAndBoardTypePaged(Long festivalId, BoardType boardType, Long cursor, int size) {
        Festival festival = EntityFinder.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        Set<Long> certifiedUserIds = certificationService.findApprovedUserIdsByFestivalId(festivalId);
        int page = CursorPage.toPage(cursor);
        Page<Post> result = postRepository.findByFestivalAndBoardTypeOrderByCreatedAtDesc(festival, boardType, PageRequest.of(page, size));
        List<PostResponseDto> content = result.getContent().stream()
                .map(post -> PostResponseDto.from(post, certifiedUserIds.contains(post.getUserId())))
                .toList();
        return CursorPage.of(result, content, cursor);
    }

    @Override
    @Transactional
    public Long createFestivalTypedPost(Long festivalId, PostRequestDto dto, Long userId, BoardType boardType) {
        Festival festival = EntityFinder.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");
        Long postId = postRepository.save(buildPost(dto, user, new PostContext(boardType, null, festival))).getId();
        eventPublisher.publishEvent(new PostCreatedEvent(userId, postId));
        return postId;
    }

    @Override
    public List<PostResponseDto> getPopularFestivalPosts(Long festivalId) {
        Festival festival = EntityFinder.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        Set<Long> certifiedUserIds = certificationService.findApprovedUserIdsByFestivalId(festivalId);
        return postRepository.findByFestivalOrderByLikeCountDesc(festival, PageRequest.of(0, PageSize.POSTS))
                .map(post -> PostResponseDto.from(post, certifiedUserIds.contains(post.getUserId())))
                .toList();
    }

    @Override
    @Transactional
    public void incrementViewCount(Long postId) {
        Post post = EntityFinder.getOrThrow(postRepository::findById, postId, "게시글");
        post.incrementViewCount();
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

    private LocalDateTime oneWeekAgo() {
        return LocalDateTime.now().minusWeeks(1);
    }

    private void validatePostContent(PostRequestDto dto) {
        badWordFilter.validateField("title", dto.getTitle());
        badWordFilter.validateField("content", dto.getContent());
    }
}
