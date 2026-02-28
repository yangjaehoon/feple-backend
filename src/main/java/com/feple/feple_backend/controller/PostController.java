package com.feple.feple_backend.controller;

import com.feple.feple_backend.domain.post.BoardType;
import com.feple.feple_backend.user.domain.User;
import com.feple.feple_backend.dto.post.PostRequestDto;
import com.feple.feple_backend.dto.post.PostResponseDto;
import com.feple.feple_backend.user.repository.UserRepository;
import com.feple.feple_backend.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;
    private final UserRepository userRepository;

    @PostMapping("/free")
    public ResponseEntity<Long> createFreePost(@RequestBody PostRequestDto dto) {
        dto.setBoardType(BoardType.FREE);

        User author = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("해당 사용자가 없습니다: " + dto.getUserId()));

        return ResponseEntity.ok(postService.createPost(dto, author));
    }

    @GetMapping("/free")
    public ResponseEntity<List<PostResponseDto>> getFreePosts() {
        return ResponseEntity.ok(postService.getPostsByBoardType(BoardType.FREE));
    }

    @PostMapping("/mate")
    public ResponseEntity<Long> createMatePost(@RequestBody PostRequestDto dto) {
        dto.setBoardType(BoardType.MATE);

        User author = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("해당 사용자가 없습니다: " + dto.getUserId()));

        return ResponseEntity.ok(postService.createPost(dto, author));
    }

    @GetMapping("/mate")
    public ResponseEntity<List<PostResponseDto>> getMatePosts() {
        return ResponseEntity.ok(postService.getPostsByBoardType(BoardType.MATE));
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<Boolean> toggleLike(@PathVariable Long postId, @RequestParam Long userId) {
        boolean liked = postService.toggleLike(postId, userId);
        return ResponseEntity.ok(liked);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId) {
        postService.deletePost(postId);
        return ResponseEntity.noContent().build();
    }

}
