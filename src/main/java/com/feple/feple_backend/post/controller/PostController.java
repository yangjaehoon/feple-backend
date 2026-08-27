package com.feple.feple_backend.post.controller;

import com.feple.feple_backend.file.ImageUploadPolicy;
import com.feple.feple_backend.file.S3PathConstants;
import com.feple.feple_backend.file.dto.S3PresignedUrlResult;
import com.feple.feple_backend.file.service.S3PresignService;
import com.feple.feple_backend.post.dto.CursorPage;
import com.feple.feple_backend.post.dto.CursorPageRequest;
import com.feple.feple_backend.post.dto.PostDraftRequestDto;
import com.feple.feple_backend.post.dto.PostDraftResponseDto;
import com.feple.feple_backend.post.dto.PostRequestDto;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.service.PostDraftService;
import com.feple.feple_backend.post.service.PostLikeService;
import com.feple.feple_backend.post.service.PostScrapService;
import com.feple.feple_backend.post.service.PostSearchService;
import com.feple.feple_backend.post.service.PostService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "게시글", description = "자유·동행·아티스트·페스티벌 게시글 CRUD, 좋아요, 스크랩")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;
    private final PostSearchService postSearchService;
    private final PostLikeService postLikeService;
    private final PostScrapService postScrapService;
    private final PostDraftService postDraftService;
    private final S3PresignService s3PresignService;

    // ── 단건 조회 / 작성 / 수정 / 삭제 ──────────────────────────────────────

    @GetMapping("/{postId}")
    public ResponseEntity<PostResponseDto> getPost(@PathVariable Long postId) {
        return ResponseEntity.ok(postService.getPost(postId));
    }

    @PostMapping("/free")
    public ResponseEntity<Long> createFreePost(@Valid @RequestBody PostRequestDto dto,
                                               @AuthenticationPrincipal Long userId) {
        return created(postService.createPost(dto, userId, BoardType.FREE));
    }

    @PostMapping("/mate")
    public ResponseEntity<Long> createMatePost(@Valid @RequestBody PostRequestDto dto,
                                               @AuthenticationPrincipal Long userId) {
        return created(postService.createPost(dto, userId, BoardType.MATE));
    }

    @PutMapping("/{postId}")
    public ResponseEntity<Void> updatePost(@PathVariable Long postId,
                                           @Valid @RequestBody PostRequestDto dto,
                                           @AuthenticationPrincipal Long userId) {
        postService.updateOwnPost(postId, dto, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId,
                                           @AuthenticationPrincipal Long userId) {
        postService.deleteOwnPost(postId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/view")
    public ResponseEntity<Void> incrementView(@PathVariable Long postId) {
        postService.incrementViewCount(postId);
        return ResponseEntity.noContent().build();
    }

    // ── 목록 조회 ──────────────────────────────────────────────────────────

    @GetMapping("/popular")
    public ResponseEntity<List<PostResponseDto>> getPopularPosts(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(postService.getPopularPosts(userId));
    }

    @GetMapping("/free")
    public ResponseEntity<CursorPage<PostResponseDto>> getFreePosts(PostPageQuery query,
                                                                    @AuthenticationPrincipal Long userId) {
        return boardTypePage(BoardType.FREE, query, userId);
    }

    @GetMapping("/mate")
    public ResponseEntity<CursorPage<PostResponseDto>> getMatePosts(PostPageQuery query,
                                                                    @AuthenticationPrincipal Long userId) {
        return boardTypePage(BoardType.MATE, query, userId);
    }

    @GetMapping("/tag/{tag}")
    public ResponseEntity<CursorPage<PostResponseDto>> getPostsByTag(@PathVariable String tag,
                                                                     PostPageQuery query,
                                                                     @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(postService.getPostsByTagPaged(tag, query.toPageRequest(userId)));
    }

    @GetMapping("/artist/{artistId}")
    public ResponseEntity<CursorPage<PostResponseDto>> getArtistPosts(@PathVariable Long artistId,
                                                                      PostPageQuery query,
                                                                      @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(postService.getPostsByArtistIdPaged(artistId, query.toPageRequest(userId)));
    }

    @GetMapping("/search")
    public ResponseEntity<List<PostResponseDto>> searchPosts(
            @RequestParam @NotBlank @Size(max = 100, message = "검색어는 100자 이내로 입력해주세요.") String keyword,
            @RequestParam(required = false) String boardType,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(postSearchService.searchPosts(keyword, boardType, userId));
    }

    // ── 아티스트 게시판 작성 ───────────────────────────────────────────────

    @PostMapping("/artist/{artistId}")
    public ResponseEntity<Long> createArtistPost(@PathVariable Long artistId,
                                                 @Valid @RequestBody PostRequestDto dto,
                                                 @AuthenticationPrincipal Long userId) {
        return created(postService.createArtistPost(artistId, dto, userId));
    }

    // ── 페스티벌 게시판 ────────────────────────────────────────────────────

    @GetMapping("/festival/{festivalId}")
    public ResponseEntity<CursorPage<PostResponseDto>> getFestivalPosts(@PathVariable Long festivalId,
                                                                        PostPageQuery query,
                                                                        @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(postService.getPostsByFestivalIdPaged(festivalId, query.toPageRequest(userId)));
    }

    @PostMapping("/festival/{festivalId}")
    public ResponseEntity<Long> createFestivalPost(@PathVariable Long festivalId,
                                                   @Valid @RequestBody PostRequestDto dto,
                                                   @AuthenticationPrincipal Long userId) {
        return created(postService.createFestivalPost(festivalId, dto, userId));
    }

    @GetMapping("/festival/{festivalId}/popular")
    public ResponseEntity<List<PostResponseDto>> getPopularFestivalPosts(@PathVariable Long festivalId,
                                                                         @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(postService.getPopularFestivalPosts(festivalId, userId));
    }

    @GetMapping("/festival/{festivalId}/companion")
    public ResponseEntity<CursorPage<PostResponseDto>> getFestivalCompanionPosts(@PathVariable Long festivalId,
                                                                                 PostPageQuery query,
                                                                                 @AuthenticationPrincipal Long userId) {
        return festivalBoardPage(festivalId, BoardType.FESTIVAL_COMPANION, query, userId);
    }

    @PostMapping("/festival/{festivalId}/companion")
    public ResponseEntity<Long> createFestivalCompanionPost(@PathVariable Long festivalId,
                                                            @Valid @RequestBody PostRequestDto dto,
                                                            @AuthenticationPrincipal Long userId) {
        return created(postService.createFestivalTypedPost(festivalId, dto, userId, BoardType.FESTIVAL_COMPANION));
    }

    @GetMapping("/festival/{festivalId}/ticket")
    public ResponseEntity<CursorPage<PostResponseDto>> getFestivalTicketPosts(@PathVariable Long festivalId,
                                                                              PostPageQuery query,
                                                                              @AuthenticationPrincipal Long userId) {
        return festivalBoardPage(festivalId, BoardType.FESTIVAL_TICKET, query, userId);
    }

    @PostMapping("/festival/{festivalId}/ticket")
    public ResponseEntity<Long> createFestivalTicketPost(@PathVariable Long festivalId,
                                                         @Valid @RequestBody PostRequestDto dto,
                                                         @AuthenticationPrincipal Long userId) {
        return created(postService.createFestivalTypedPost(festivalId, dto, userId, BoardType.FESTIVAL_TICKET));
    }

    // ── 좋아요 ─────────────────────────────────────────────────────────────

    @GetMapping("/{postId}/liked")
    public ResponseEntity<Boolean> isLiked(@PathVariable Long postId,
                                           @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(postLikeService.isLikedByUser(postId, userId));
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<Boolean> toggleLike(@PathVariable Long postId,
                                              @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(postLikeService.toggleLike(postId, userId));
    }

    // ── 스크랩 ─────────────────────────────────────────────────────────────

    @GetMapping("/{postId}/scraped")
    public ResponseEntity<Boolean> isScraped(@PathVariable Long postId,
                                             @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(postScrapService.isScrapedByUser(postId, userId));
    }

    @PostMapping("/{postId}/scrap")
    public ResponseEntity<Boolean> toggleScrap(@PathVariable Long postId,
                                               @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(postScrapService.toggleScrap(postId, userId));
    }

    @GetMapping("/scrapped")
    public ResponseEntity<List<PostResponseDto>> getMyScraps(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(postScrapService.getMyScraps(userId));
    }

    // ── 이미지 업로드 URL ──────────────────────────────────────────────────

    record PostImagePresignRequest(
            @NotBlank String contentType,
            @NotBlank String extension) {}

    @PostMapping("/image-upload-url")
    public ResponseEntity<S3PresignedUrlResult> getPostImageUploadUrl(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PostImagePresignRequest req) {
        String ext = ImageUploadPolicy.assertAllowed(req.extension(), req.contentType());
        String key = S3PathConstants.postImagePrefix(userId) + UUID.randomUUID() + "." + ext;
        return ResponseEntity.ok(s3PresignService.presignPut(key, req.contentType()));
    }

    // ── 임시저장 ───────────────────────────────────────────────────────────

    @PutMapping("/draft")
    public ResponseEntity<Void> saveDraft(@Valid @RequestBody PostDraftRequestDto dto,
                                          @AuthenticationPrincipal Long userId) {
        postDraftService.saveDraft(userId, dto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/draft")
    public ResponseEntity<PostDraftResponseDto> getDraft(@AuthenticationPrincipal Long userId) {
        return postDraftService.getDraft(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @DeleteMapping("/draft")
    public ResponseEntity<Void> deleteDraft(@AuthenticationPrincipal Long userId) {
        postDraftService.deleteDraft(userId);
        return ResponseEntity.noContent().build();
    }

    // ── 내부 헬퍼 ──────────────────────────────────────────────────────────

    private ResponseEntity<CursorPage<PostResponseDto>> boardTypePage(BoardType boardType, PostPageQuery query,
                                                                      Long userId) {
        CursorPageRequest pageRequest = query.toPageRequest(userId);
        return ResponseEntity.ok(query.isPopular()
                ? postService.getPostsByBoardTypePopular(boardType, pageRequest)
                : postService.getPostsByBoardTypeLatest(boardType, pageRequest));
    }

    private ResponseEntity<CursorPage<PostResponseDto>> festivalBoardPage(Long festivalId, BoardType boardType,
                                                                          PostPageQuery query, Long userId) {
        return ResponseEntity.ok(
                postService.getPostsByFestivalIdAndBoardTypePaged(festivalId, boardType, query.toPageRequest(userId)));
    }

    private static ResponseEntity<Long> created(Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(id);
    }
}
