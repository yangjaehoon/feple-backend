package com.feple.feple_backend.post.controller;

import com.feple.feple_backend.file.service.S3PresignService;
import com.feple.feple_backend.file.dto.S3PresignedUrlResult;
import com.feple.feple_backend.global.PageSize;
import com.feple.feple_backend.global.exception.ErrorCode;
import com.feple.feple_backend.global.exception.ErrorResponse;
import com.feple.feple_backend.post.dto.CursorPage;
import com.feple.feple_backend.post.dto.CursorPageRequest;
import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.dto.PostRequestDto;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.service.PostLikeService;
import com.feple.feple_backend.post.service.PostScrapService;
import com.feple.feple_backend.post.service.PostSearchService;
import com.feple.feple_backend.post.service.PostService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    private final S3PresignService s3PresignService;

    @GetMapping("/{postId}")
    public ResponseEntity<PostResponseDto> getPost(@PathVariable Long postId) {
        return ResponseEntity.ok(postService.getPost(postId));
    }

    @GetMapping("/{postId}/liked")
    public ResponseEntity<Boolean> isLiked(@PathVariable Long postId,
                                           @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(postLikeService.isLikedByUser(postId, userId));
    }

    @PostMapping("/free")
    public ResponseEntity<Long> createFreePost(@Valid @RequestBody PostRequestDto dto,
                                               @AuthenticationPrincipal Long userId) {
        dto.setBoardType(BoardType.FREE);
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.createPost(dto, userId));
    }

    @GetMapping("/popular")
    public ResponseEntity<List<PostResponseDto>> getPopularPosts(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(postService.getPopularPosts(userId));
    }

    @GetMapping("/free")
    public ResponseEntity<CursorPage<PostResponseDto>> getFreePosts(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "latest") String sort,
            @AuthenticationPrincipal Long userId) {
        CursorPageRequest pageRequest = new CursorPageRequest(cursor, Math.min(size, PageSize.MAX_PAGE_SIZE), userId);
        if ("popular".equals(sort)) {
            return ResponseEntity.ok(postService.getPostsByBoardTypePopular(BoardType.FREE, pageRequest));
        }
        return ResponseEntity.ok(postService.getPostsByBoardTypeLatest(BoardType.FREE, pageRequest));
    }

    @PostMapping("/mate")
    public ResponseEntity<Long> createMatePost(@Valid @RequestBody PostRequestDto dto,
                                               @AuthenticationPrincipal Long userId) {
        dto.setBoardType(BoardType.MATE);
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.createPost(dto, userId));
    }

    @GetMapping("/mate")
    public ResponseEntity<CursorPage<PostResponseDto>> getMatePosts(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "latest") String sort,
            @AuthenticationPrincipal Long userId) {
        CursorPageRequest pageRequest = new CursorPageRequest(cursor, Math.min(size, PageSize.MAX_PAGE_SIZE), userId);
        if ("popular".equals(sort)) {
            return ResponseEntity.ok(postService.getPostsByBoardTypePopular(BoardType.MATE, pageRequest));
        }
        return ResponseEntity.ok(postService.getPostsByBoardTypeLatest(BoardType.MATE, pageRequest));
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<Boolean> toggleLike(@PathVariable Long postId,
                                              @AuthenticationPrincipal Long userId) {
        boolean liked = postLikeService.toggleLike(postId, userId);
        return ResponseEntity.ok(liked);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId,
                                           @AuthenticationPrincipal Long userId) {
        postService.deleteOwnPost(postId, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{postId}")
    public ResponseEntity<Void> updatePost(@PathVariable Long postId,
                                            @Valid @RequestBody PostRequestDto dto,
                                            @AuthenticationPrincipal Long userId) {
        postService.updateOwnPost(postId, dto, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<PostResponseDto>> searchPosts(
            @RequestParam @NotBlank @Size(max = 100, message = "검색어는 100자 이내로 입력해주세요.") String keyword,
            @RequestParam(required = false) String boardType,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(postSearchService.searchPosts(keyword, boardType, userId));
    }

    // ── 스크랩 ──

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

    @GetMapping("/artist/{artistId}")
    public ResponseEntity<CursorPage<PostResponseDto>> getArtistPosts(
            @PathVariable Long artistId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(postService.getPostsByArtistIdPaged(artistId,
                new CursorPageRequest(cursor, Math.min(size, PageSize.MAX_PAGE_SIZE), userId)));
    }

    @PostMapping("/artist/{artistId}")
    public ResponseEntity<Long> createArtistPost(@PathVariable Long artistId,
                                                  @Valid @RequestBody PostRequestDto dto,
                                                  @AuthenticationPrincipal Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.createArtistPost(artistId, dto, userId));
    }

    @GetMapping("/festival/{festivalId}")
    public ResponseEntity<CursorPage<PostResponseDto>> getFestivalPosts(
            @PathVariable Long festivalId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(postService.getPostsByFestivalIdPaged(festivalId,
                new CursorPageRequest(cursor, Math.min(size, PageSize.MAX_PAGE_SIZE), userId)));
    }

    @PostMapping("/festival/{festivalId}")
    public ResponseEntity<Long> createFestivalPost(@PathVariable Long festivalId,
                                                    @Valid @RequestBody PostRequestDto dto,
                                                    @AuthenticationPrincipal Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.createFestivalPost(festivalId, dto, userId));
    }

    @GetMapping("/festival/{festivalId}/popular")
    public ResponseEntity<List<PostResponseDto>> getPopularFestivalPosts(
            @PathVariable Long festivalId, @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(postService.getPopularFestivalPosts(festivalId, userId));
    }

    @GetMapping("/festival/{festivalId}/companion")
    public ResponseEntity<CursorPage<PostResponseDto>> getFestivalCompanionPosts(
            @PathVariable Long festivalId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(postService.getPostsByFestivalIdAndBoardTypePaged(festivalId, BoardType.FESTIVAL_COMPANION,
                new CursorPageRequest(cursor, Math.min(size, PageSize.MAX_PAGE_SIZE), userId)));
    }

    @PostMapping("/festival/{festivalId}/companion")
    public ResponseEntity<Long> createFestivalCompanionPost(@PathVariable Long festivalId,
                                                             @Valid @RequestBody PostRequestDto dto,
                                                             @AuthenticationPrincipal Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.createFestivalTypedPost(festivalId, dto, userId, BoardType.FESTIVAL_COMPANION));
    }

    @GetMapping("/festival/{festivalId}/ticket")
    public ResponseEntity<CursorPage<PostResponseDto>> getFestivalTicketPosts(
            @PathVariable Long festivalId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(postService.getPostsByFestivalIdAndBoardTypePaged(festivalId, BoardType.FESTIVAL_TICKET,
                new CursorPageRequest(cursor, Math.min(size, PageSize.MAX_PAGE_SIZE), userId)));
    }

    @PostMapping("/festival/{festivalId}/ticket")
    public ResponseEntity<Long> createFestivalTicketPost(@PathVariable Long festivalId,
                                                          @Valid @RequestBody PostRequestDto dto,
                                                          @AuthenticationPrincipal Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.createFestivalTypedPost(festivalId, dto, userId, BoardType.FESTIVAL_TICKET));
    }

    @PostMapping("/{postId}/view")
    public ResponseEntity<Void> incrementView(@PathVariable Long postId) {
        postService.incrementViewCount(postId);
        return ResponseEntity.noContent().build();
    }

    // extension → 허용 content-type 매핑으로 확장자·MIME 불일치 업로드 차단
    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
            "jpg",  "image/jpeg",
            "jpeg", "image/jpeg",
            "png",  "image/png",
            "webp", "image/webp"
    );

    record PostImagePresignRequest(
            @NotBlank String contentType,
            @NotBlank String extension) {}

    @PostMapping("/image-upload-url")
    public ResponseEntity<?> getPostImageUploadUrl(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PostImagePresignRequest req) {
        if (!req.contentType().equals(ALLOWED_IMAGE_TYPES.get(req.extension()))) {
            return ResponseEntity.badRequest().body(
                ErrorResponse.of(HttpStatus.BAD_REQUEST, "파일 형식과 Content-Type이 일치하지 않습니다.", ErrorCode.ILLEGAL_ARGUMENT));
        }
        String key = "posts/" + userId + "/" + UUID.randomUUID() + "." + req.extension();
        return ResponseEntity.ok(s3PresignService.presignPut(key, req.contentType()));
    }

}
