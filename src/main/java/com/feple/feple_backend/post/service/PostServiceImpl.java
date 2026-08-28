package com.feple.feple_backend.post.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.badword.BadWordValidator;
import com.feple.feple_backend.certification.service.FestivalCertificationService;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.file.S3PathConstants;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.file.service.S3ObjectVerificationService;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.global.OwnershipValidator;
import com.feple.feple_backend.global.PageSize;
import com.feple.feple_backend.global.exception.ResourceNotFoundException;
import com.feple.feple_backend.post.dto.CursorPage;
import com.feple.feple_backend.post.dto.CursorPageRequest;
import com.feple.feple_backend.post.dto.PostRequestDto;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.entity.PostTag;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.post.repository.PostTagRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import com.feple.feple_backend.userblock.service.BlockedContentFilter;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final PostTagRepository postTagRepository;
    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;
    private final FestivalRepository festivalRepository;
    private final FestivalCertificationService certificationService;
    private final BadWordValidator badWordFilter;
    private final PopularPostCache popularPostCache;
    private final BlockedContentFilter blockedContentFilter;
    private final S3ObjectVerificationService s3ObjectVerificationService;
    private final FileStorageService fileStorageService;
    // DB 영속 로직은 별도 트랜잭션 경계(PostWriter)에 위임한다 — 아래 create/update 진입점은
    // 트랜잭션 밖에서 검증(S3 오브젝트 확인 등 외부 I/O 포함)을 끝내고 이 빈을 호출한다.
    private final PostWriter postWriter;

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Long createPost(PostRequestDto dto, Long userId, BoardType boardType) {
        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");
        return savePost(dto, user, new PostContext(boardType, null, null));
    }

    @Override
    public PostResponseDto getPost(Long postId) {
        Post post = EntityLoader.getOrThrow(postRepository::findWithAssociationsById, postId, "게시글");
        return PostResponseDto.from(post, fileStorageService);
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
                this::toDtos);
    }

    @Override
    public CursorPage<PostResponseDto> getPostsByBoardTypePopular(BoardType boardType, CursorPageRequest pageRequest) {
        // likeCount는 동적으로 변하므로 offset 기반 유지, cursor는 페이지 번호를 opaque Long으로 전달
        Long cursor = pageRequest.cursor();
        int page = CursorPage.toPage(cursor);
        Page<Post> result = postRepository.findByBoardTypeOrderByLikeCountDescCreatedAtDescIdDesc(
                boardType, PageRequest.of(page, pageRequest.size()));
        List<PostResponseDto> content = blockedContentFilter.excludeBlocked(
                result.map(post -> PostResponseDto.from(post, fileStorageService)).toList(), pageRequest.viewerId(), PostResponseDto::getUserId);
        return CursorPage.of(result, content, cursor);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void updateOwnPost(Long postId, PostRequestDto dto, Long requestUserId) {
        Post post = EntityLoader.getOrThrow(postRepository::findById, postId, "게시글");
        OwnershipValidator.checkOwner(post.getUserId(), requestUserId, "게시글", "수정");
        validatePostContent(dto);
        validateImageUrls(dto.getImageUrls(), requestUserId);
        postWriter.update(postId, dto);
    }

    @Override
    @Transactional
    public void deleteOwnPost(Long postId, Long requestUserId) {
        Post post = EntityLoader.getOrThrow(postRepository::findById, postId, "게시글");
        OwnershipValidator.checkOwner(post.getUserId(), requestUserId, "게시글");
        // @SQLDelete가 걸려 있어 remove가 deleted_at 세팅(소프트 삭제)로 동작한다 — 행이 남아
        // FK 무결성 유지, 신고 등 증거 보존.
        postRepository.delete(post);
    }

    @Override
    public CursorPage<PostResponseDto> getPostsByArtistIdPaged(Long artistId, CursorPageRequest pageRequest) {
        Artist artist = EntityLoader.getOrThrow(artistRepository::findById, artistId, "아티스트");
        Long cursor = pageRequest.cursor();
        return buildCursorPage(pageRequest,
                limit -> postRepository.findByArtistOrderByIdDesc(artist, limit),
                limit -> postRepository.findByArtistAndIdLessThanOrderByIdDesc(artist, cursor, limit),
                () -> postRepository.findByArtistAndPinnedTrueOrderByCreatedAtDesc(artist, PageRequest.of(0, PageSize.PINNED_POSTS)),
                this::toDtos);
    }

    @Override
    public CursorPage<PostResponseDto> getPostsByTagPaged(String tag, CursorPageRequest pageRequest) {
        String normalized = PostTags.normalize(tag);
        Long cursor = pageRequest.cursor();
        // 태그 쿼리가 JOIN t.post로 삭제·블라인드된 게시글을 이미 제외한다.
        return buildCursorPage(pageRequest,
                limit -> postTagRepository.findByTagOrderByPostIdDesc(normalized, limit).stream()
                        .map(PostTag::getPost).toList(),
                limit -> postTagRepository.findByTagAndPostIdLessThanOrderByPostIdDesc(normalized, cursor, limit).stream()
                        .map(PostTag::getPost).toList(),
                List::of,
                this::toDtos);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Long createArtistPost(Long artistId, PostRequestDto dto, Long userId) {
        Artist artist = EntityLoader.getOrThrow(artistRepository::findById, artistId, "아티스트");
        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");
        return savePost(dto, user, new PostContext(null, artist, null));
    }

    @Override
    public CursorPage<PostResponseDto> getPostsByFestivalIdPaged(Long festivalId, CursorPageRequest pageRequest) {
        Festival festival = EntityLoader.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        Long cursor = pageRequest.cursor();
        return buildCursorPage(pageRequest,
                limit -> postRepository.findGeneralFestivalPostsOrderByIdDesc(festival, limit),
                limit -> postRepository.findGeneralFestivalPostsAndIdLessThanOrderByIdDesc(festival, cursor, limit),
                () -> postRepository.findGeneralFestivalPinnedPostsOrderByCreatedAtDesc(festival, PageRequest.of(0, PageSize.PINNED_POSTS)),
                posts -> toFestivalDtos(festivalId, posts));
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Long createFestivalPost(Long festivalId, PostRequestDto dto, Long userId) {
        Festival festival = EntityLoader.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");
        return savePost(dto, user, new PostContext(null, null, festival));
    }

    @Override
    public CursorPage<PostResponseDto> getPostsByFestivalIdAndBoardTypePaged(Long festivalId, BoardType boardType, CursorPageRequest pageRequest) {
        Festival festival = EntityLoader.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        Long cursor = pageRequest.cursor();
        return buildCursorPage(pageRequest,
                limit -> postRepository.findByFestivalAndBoardTypeOrderByIdDesc(festival, boardType, limit),
                limit -> postRepository.findByFestivalAndBoardTypeAndIdLessThanOrderByIdDesc(festival, boardType, cursor, limit),
                () -> postRepository.findByFestivalAndBoardTypeAndPinnedTrueOrderByCreatedAtDesc(festival, boardType, PageRequest.of(0, PageSize.PINNED_POSTS)),
                posts -> toFestivalDtos(festivalId, posts));
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Long createFestivalTypedPost(Long festivalId, PostRequestDto dto, Long userId, BoardType boardType) {
        Festival festival = EntityLoader.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");
        return savePost(dto, user, new PostContext(boardType, null, festival));
    }

    @Override
    public List<PostResponseDto> getPopularFestivalPosts(Long festivalId, Long viewerId) {
        Festival festival = EntityLoader.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        List<Post> posts = postRepository.findByFestivalOrderByLikeCountDesc(festival, PageRequest.of(0, PageSize.POSTS)).getContent();
        return blockedContentFilter.excludeBlocked(
                toFestivalDtos(festivalId, posts), viewerId, PostResponseDto::getUserId);
    }

    @Override
    @Transactional
    public void incrementViewCount(Long postId) {
        // 존재 확인용 SELECT 없이 UPDATE 적중 행 수로 404 판단 (1회 쿼리)
        if (postRepository.incrementViewCount(postId) == 0) {
            throw ResourceNotFoundException.of("게시글", postId);
        }
    }

    // 클래스 기본값(readOnly=true)을 오버라이드하지 않으면, 이 메서드가 트랜잭션을 여는
    // 진입점으로 호출될 경우 물리 커넥션이 읽기 전용으로 열려 아래 @Modifying UPDATE가
    // 조용히 무시될 수 있다 — incrementViewCount와 동일하게 명시적으로 쓰기 트랜잭션을 연다.
    @Override
    @Transactional
    public void incrementCommentCount(Long postId) {
        postRepository.incrementCommentCount(postId);
    }

    @Override
    @Transactional
    public void decrementCommentCount(Long postId) {
        postRepository.decrementCommentCount(postId);
    }

    // fetchFirst/fetchAfterCursor는 cursor==null 여부에 따라 호출부에서 다른 리포지토리 메서드를 넘긴다.
    // 고정글(fetchPinned)은 첫 페이지(cursor==null)에만 상단에 붙인다 — 매 페이지마다 붙이면
    // 커서 페이지네이션 특성상 넘길 때마다 같은 고정글이 반복 노출된다.
    // pageMapper는 한 페이지분 Post 전체를 받는다 — 인증 뱃지처럼 "이 페이지 작성자들"에 대해서만
    // 조회하면 되는 부가 정보를 페이지 단위 쿼리 1회로 해결하기 위함(페스티벌 전체 인증자 로드 방지).
    private CursorPage<PostResponseDto> buildCursorPage(CursorPageRequest pageRequest,
                                                          Function<PageRequest, List<Post>> fetchFirst,
                                                          Function<PageRequest, List<Post>> fetchAfterCursor,
                                                          Supplier<List<Post>> fetchPinned,
                                                          Function<List<Post>, List<PostResponseDto>> pageMapper) {
        return CursorPageAssembler.assemble(pageRequest.cursor(), pageRequest.size(), fetchFirst, fetchAfterCursor,
                pageItems -> {
                    List<Post> combined = pageRequest.cursor() == null
                            ? Stream.concat(fetchPinned.get().stream(), pageItems.stream()).toList()
                            : pageItems;
                    return blockedContentFilter.excludeBlocked(
                            pageMapper.apply(combined), pageRequest.viewerId(), PostResponseDto::getUserId);
                },
                Post::getId);
    }

    private List<PostResponseDto> toDtos(List<Post> posts) {
        return posts.stream().map(post -> PostResponseDto.from(post, fileStorageService)).toList();
    }

    // 페스티벌 게시글 목록: 인증 뱃지를 "이 목록에 등장하는 작성자"에 한해 한 번에 조회한다.
    private List<PostResponseDto> toFestivalDtos(Long festivalId, List<Post> posts) {
        List<Long> authorIds = posts.stream().map(Post::getUserId).distinct().toList();
        Set<Long> certified = certificationService.findApprovedUserIdsByFestivalId(festivalId, authorIds);
        return posts.stream()
                .map(post -> PostResponseDto.from(post, certified.contains(post.getUserId()), fileStorageService))
                .toList();
    }

    // 트랜잭션 밖(NOT_SUPPORTED)에서 실행된다 — 검증(외부 I/O 포함)을 끝낸 뒤 DB 저장은
    // PostWriter의 트랜잭션 경계에 위임한다.
    private Long savePost(PostRequestDto dto, User user, PostContext ctx) {
        validatePostContent(dto);
        validateImageUrls(dto.getImageUrls(), user.getId());
        return postWriter.save(dto, user, ctx);
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
