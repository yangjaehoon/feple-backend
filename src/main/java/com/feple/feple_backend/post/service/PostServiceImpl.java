package com.feple.feple_backend.post.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.badword.BadWordValidator;
import com.feple.feple_backend.certification.service.FestivalCertificationService;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.file.S3PathConstants;
import com.feple.feple_backend.file.service.S3ObjectVerificationService;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.global.OwnershipValidator;
import com.feple.feple_backend.global.PageSize;
import com.feple.feple_backend.post.dto.CursorPage;
import com.feple.feple_backend.post.dto.CursorPageRequest;
import com.feple.feple_backend.post.dto.PostRequestDto;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.entity.PostImage;
import com.feple.feple_backend.post.event.PostCreatedEvent;
import com.feple.feple_backend.post.repository.PostImageRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import com.feple.feple_backend.userblock.service.BlockedContentFilter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;
    private final FestivalRepository festivalRepository;
    private final FestivalCertificationService certificationService;
    private final BadWordValidator badWordFilter;
    private final ApplicationEventPublisher eventPublisher;
    private final PopularPostCache popularPostCache;
    private final BlockedContentFilter blockedContentFilter;
    private final S3ObjectVerificationService s3ObjectVerificationService;

    private record PostContext(BoardType boardType, Artist artist, Festival festival) {}

    @Override
    @Transactional
    public Long createPost(PostRequestDto dto, Long userId) {
        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");
        Long postId = savePost(dto, user, new PostContext(dto.getBoardType(), null, null));
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
        List<PostResponseDto> pool = blockedContentFilter.excludeBlocked(
                popularPostCache.getPopularPosts(), viewerId, PostResponseDto::getUserId);
        return pool.stream().limit(PageSize.POPULAR_POSTS).toList();
    }

    @Override
    public CursorPage<PostResponseDto> getPostsByBoardTypeLatest(BoardType boardType, CursorPageRequest pageRequest) {
        Long cursor = pageRequest.cursor();
        return buildCursorPage(pageRequest,
                limit -> postRepository.findByBoardTypeAndPinnedFalseOrderByIdDesc(boardType, limit),
                limit -> postRepository.findByBoardTypeAndPinnedFalseAndIdLessThanOrderByIdDesc(boardType, cursor, limit),
                () -> postRepository.findByBoardTypeAndPinnedTrueOrderByCreatedAtDesc(boardType, PageRequest.of(0, PageSize.PINNED_POSTS)),
                PostResponseDto::from);
    }

    @Override
    public CursorPage<PostResponseDto> getPostsByBoardTypePopular(BoardType boardType, CursorPageRequest pageRequest) {
        // likeCount는 동적으로 변하므로 offset 기반 유지, cursor는 페이지 번호를 opaque Long으로 전달
        Long cursor = pageRequest.cursor();
        int page = CursorPage.toPage(cursor);
        Page<Post> result = postRepository.findByBoardTypeOrderByLikeCountDescCreatedAtDescIdDesc(
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
        validateImageUrls(dto.getImageUrls(), requestUserId);
        post.update(dto.getTitle(), dto.getContent());
        postImageRepository.deleteByPostId(postId);
        saveImages(post, dto.getImageUrls());
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
                () -> postRepository.findByArtistAndPinnedTrueOrderByCreatedAtDesc(artist, PageRequest.of(0, PageSize.PINNED_POSTS)),
                PostResponseDto::from);
    }

    @Override
    @Transactional
    public Long createArtistPost(Long artistId, PostRequestDto dto, Long userId) {
        Artist artist = EntityLoader.getOrThrow(artistRepository::findById, artistId, "아티스트");
        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");
        Long postId = savePost(dto, user, new PostContext(null, artist, null));
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
                () -> postRepository.findGeneralFestivalPinnedPostsOrderByCreatedAtDesc(festival, PageRequest.of(0, PageSize.PINNED_POSTS)),
                post -> PostResponseDto.from(post, certifiedUserIds.contains(post.getUserId())));
    }

    @Override
    @Transactional
    public Long createFestivalPost(Long festivalId, PostRequestDto dto, Long userId) {
        Festival festival = EntityLoader.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");
        Long postId = savePost(dto, user, new PostContext(null, null, festival));
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
                () -> postRepository.findByFestivalAndBoardTypeAndPinnedTrueOrderByCreatedAtDesc(festival, boardType, PageRequest.of(0, PageSize.PINNED_POSTS)),
                post -> PostResponseDto.from(post, certifiedUserIds.contains(post.getUserId())));
    }

    @Override
    @Transactional
    public Long createFestivalTypedPost(Long festivalId, PostRequestDto dto, Long userId, BoardType boardType) {
        Festival festival = EntityLoader.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");
        Long postId = savePost(dto, user, new PostContext(boardType, null, festival));
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

    @Override
    public void incrementCommentCount(Long postId) {
        postRepository.incrementCommentCount(postId);
    }

    @Override
    public void decrementCommentCount(Long postId) {
        postRepository.decrementCommentCount(postId);
    }

    // fetchFirst/fetchAfterCursor는 cursor==null 여부에 따라 호출부에서 다른 리포지토리 메서드를 넘긴다.
    // 고정글(fetchPinned)은 첫 페이지(cursor==null)에만 상단에 붙인다 — 매 페이지마다 붙이면
    // 커서 페이지네이션 특성상 넘길 때마다 같은 고정글이 반복 노출된다.
    private CursorPage<PostResponseDto> buildCursorPage(CursorPageRequest pageRequest,
                                                          Function<PageRequest, List<Post>> fetchFirst,
                                                          Function<PageRequest, List<Post>> fetchAfterCursor,
                                                          Supplier<List<Post>> fetchPinned,
                                                          Function<Post, PostResponseDto> mapper) {
        return CursorPageAssembler.assemble(pageRequest.cursor(), pageRequest.size(), fetchFirst, fetchAfterCursor,
                pageItems -> {
                    List<Post> combined = pageRequest.cursor() == null
                            ? Stream.concat(fetchPinned.get().stream(), pageItems.stream()).toList()
                            : pageItems;
                    return blockedContentFilter.excludeBlocked(
                            combined.stream().map(mapper).toList(), pageRequest.viewerId(), PostResponseDto::getUserId);
                },
                Post::getId);
    }

    private Long savePost(PostRequestDto dto, User user, PostContext ctx) {
        validatePostContent(dto);
        validateImageUrls(dto.getImageUrls(), user.getId());
        Post saved = postRepository.save(buildPost(dto, user, ctx));
        saveImages(saved, dto.getImageUrls());
        return saved.getId();
    }

    private Post buildPost(PostRequestDto dto, User user, PostContext ctx) {
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
                .build();
    }

    private void saveImages(Post post, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) return;
        List<PostImage> images = new ArrayList<>();
        for (int i = 0; i < imageUrls.size(); i++) {
            images.add(PostImage.builder().post(post).imageKey(imageUrls.get(i)).sortOrder(i).build());
        }
        postImageRepository.saveAll(images);
    }

    private void validatePostContent(PostRequestDto dto) {
        badWordFilter.validateField("title", dto.getTitle());
        badWordFilter.validateField("content", dto.getContent());
    }

    // 클라이언트가 presign 없이 임의 문자열(타인의 S3 키·외부 URL)을 이미지로 제출하는 것을 막는다.
    // /posts/image-upload-url이 발급한 "posts/{userId}/..." 접두사 범위 내 실제 업로드된 객체만 허용.
    private void validateImageUrls(List<String> imageUrls, Long userId) {
        if (imageUrls == null) return;
        for (String imageUrl : imageUrls) {
            if (imageUrl == null || imageUrl.isBlank()) continue;
            S3PathConstants.requireWithinPrefix(imageUrl, S3PathConstants.postImagePrefix(userId));
            s3ObjectVerificationService.verifyImageObject(imageUrl);
        }
    }
}
