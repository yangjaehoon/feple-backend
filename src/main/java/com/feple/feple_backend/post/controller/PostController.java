package com.feple.feple_backend.post.controller;

import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.dto.PostRequestDto;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.service.PostLikeService;
import com.feple.feple_backend.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;
    private final PostLikeService postLikeService;

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
    public ResponseEntity<List<PostResponseDto>> getFreePosts() {
        return ResponseEntity.ok(postService.getPostsByBoardType(BoardType.FREE));
    }

    @PostMapping("/mate")
    public ResponseEntity<Long> createMatePost(@Valid @RequestBody PostRequestDto dto,
                                               @AuthenticationPrincipal Long userId) {
        dto.setBoardType(BoardType.MATE);
        return ResponseEntity.ok(postService.createPost(dto, userId));
    }

    @GetMapping("/mate")
    public ResponseEntity<List<PostResponseDto>> getMatePosts() {
        return ResponseEntity.ok(postService.getPostsByBoardType(BoardType.MATE));
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

}
