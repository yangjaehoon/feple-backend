package com.feple.feple_backend.controller;

import com.feple.feple_backend.domain.post.BoardType;
import com.feple.feple_backend.domain.post.Post;
import com.feple.feple_backend.domain.user.User;
import com.feple.feple_backend.dto.post.PostRequestDto;
import com.feple.feple_backend.dto.post.PostResponseDto;
import com.feple.feple_backend.repository.UserRepository;
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
    public ResponseEntity<Long> createMatePost(@RequestBody PostRequestDto dto, User user) {
        dto.setBoardType(BoardType.MATE);
        return ResponseEntity.ok(postService.createPost(dto, user));
    }

    @GetMapping("/mate")
    public ResponseEntity<List<PostResponseDto>> getMatePosts() {
        return ResponseEntity.ok(postService.getPostsByBoardType(BoardType.MATE));
    }

}
