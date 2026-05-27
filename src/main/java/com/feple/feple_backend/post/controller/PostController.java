package com.feple.feple_backend.post.controller;

import com.feple.feple_backend.artist.service.S3PresignService;
import com.feple.feple_backend.file.dto.PresignResult;
import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.dto.PostRequestDto;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.service.PostLikeService;
import com.feple.feple_backend.post.service.PostScrapService;
import com.feple.feple_backend.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;
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
        return ResponseEntity.ok(postService.createPost(dto, userId));
    }

    @GetMapping("/hot")
    public ResponseEntity<List<PostResponseDto>> getHotPosts() {
        return ResponseEntity.ok(postService.getHotPosts());
    }

    @GetMapping("/free")
    public ResponseEntity<List<PostResponseDto>> getFreePosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "latest") String sort) {
        if ("popular".equals(sort)) {
            return ResponseEntity.ok(postService.getPostsByBoardTypePopular(BoardType.FREE, page, Math.min(size, 50)));
        }
        return ResponseEntity.ok(postService.getPostsByBoardTypePaged(BoardType.FREE, page, Math.min(size, 50)));
    }

    @PostMapping("/mate")
    public ResponseEntity<Long> createMatePost(@Valid @RequestBody PostRequestDto dto,
                                               @AuthenticationPrincipal Long userId) {
        dto.setBoardType(BoardType.MATE);
        return ResponseEntity.ok(postService.createPost(dto, userId));
    }

    @GetMapping("/mate")
    public ResponseEntity<List<PostResponseDto>> getMatePosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "latest") String sort) {
        if ("popular".equals(sort)) {
            return ResponseEntity.ok(postService.getPostsByBoardTypePopular(BoardType.MATE, page, Math.min(size, 50)));
        }
        return ResponseEntity.ok(postService.getPostsByBoardTypePaged(BoardType.MATE, page, Math.min(size, 50)));
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

    @GetMapping("/my/scrapped")
    public ResponseEntity<List<PostResponseDto>> getMyScraps(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(postScrapService.getMyScraps(userId));
    }

    @GetMapping("/artist/{artistId}")
    public ResponseEntity<List<PostResponseDto>> getArtistPosts(@PathVariable Long artistId) {
        return ResponseEntity.ok(postService.getPostsByArtistId(artistId));
    }

    @PostMapping("/artist/{artistId}")
    public ResponseEntity<Long> createArtistPost(@PathVariable Long artistId,
                                                  @Valid @RequestBody PostRequestDto dto,
                                                  @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(postService.createArtistPost(artistId, dto, userId));
    }

    @GetMapping("/festival/{festivalId}")
    public ResponseEntity<List<PostResponseDto>> getFestivalPosts(@PathVariable Long festivalId) {
        return ResponseEntity.ok(postService.getPostsByFestivalId(festivalId));
    }

    @PostMapping("/festival/{festivalId}")
    public ResponseEntity<Long> createFestivalPost(@PathVariable Long festivalId,
                                                    @Valid @RequestBody PostRequestDto dto,
                                                    @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(postService.createFestivalPost(festivalId, dto, userId));
    }

    @GetMapping("/festival/{festivalId}/popular")
    public ResponseEntity<List<PostResponseDto>> getPopularFestivalPosts(@PathVariable Long festivalId) {
        return ResponseEntity.ok(postService.getPopularFestivalPosts(festivalId));
    }

    @GetMapping("/festival/{festivalId}/companion")
    public ResponseEntity<List<PostResponseDto>> getFestivalCompanionPosts(@PathVariable Long festivalId) {
        return ResponseEntity.ok(postService.getPostsByFestivalIdAndBoardType(festivalId, BoardType.FESTIVAL_COMPANION));
    }

    @PostMapping("/festival/{festivalId}/companion")
    public ResponseEntity<Long> createFestivalCompanionPost(@PathVariable Long festivalId,
                                                             @Valid @RequestBody PostRequestDto dto,
                                                             @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(postService.createFestivalTypedPost(festivalId, dto, userId, BoardType.FESTIVAL_COMPANION));
    }

    @GetMapping("/festival/{festivalId}/ticket")
    public ResponseEntity<List<PostResponseDto>> getFestivalTicketPosts(@PathVariable Long festivalId) {
        return ResponseEntity.ok(postService.getPostsByFestivalIdAndBoardType(festivalId, BoardType.FESTIVAL_TICKET));
    }

    @PostMapping("/festival/{festivalId}/ticket")
    public ResponseEntity<Long> createFestivalTicketPost(@PathVariable Long festivalId,
                                                          @Valid @RequestBody PostRequestDto dto,
                                                          @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(postService.createFestivalTypedPost(festivalId, dto, userId, BoardType.FESTIVAL_TICKET));
    }

    @GetMapping("/image-upload-url")
    public ResponseEntity<PresignResult> getPostImageUploadUrl(@AuthenticationPrincipal Long userId) {
        String key = "posts/" + userId + "/" + UUID.randomUUID() + ".jpg";
        return ResponseEntity.ok(s3PresignService.presignPut(key, "image/jpeg"));
    }

}
