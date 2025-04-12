package com.feple.feple_backend.controller;

import com.feple.feple_backend.domain.post.BoardType;
import com.feple.feple_backend.domain.post.Post;
import com.feple.feple_backend.dto.post.PostRequestDto;
import com.feple.feple_backend.dto.post.PostResponseDto;
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

    @PostMapping("/free")
    public ResponseEntity<Long> createFreePost(@RequestBody PostRequestDto dto) {
        dto.setBoardType(BoardType.FREE);
        return ResponseEntity.ok(postService.createPost(dto));
    }

    @GetMapping("/free")
    public ResponseEntity<List<PostResponseDto>> getFreePosts() {
        return ResponseEntity.ok(postService.getPostsByBoardType(BoardType.FREE));
    }

}
