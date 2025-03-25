package com.feple.feple_backend.controller;

import com.feple.feple_backend.domain.post.Post;
import com.feple.feple_backend.dto.post.PostRequestDto;
import com.feple.feple_backend.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<Long> createPost(@RequestBody PostRequestDto dto) {
        Long postId = postService.createPost(dto);
        return ResponseEntity.ok(postId);
    }
}
