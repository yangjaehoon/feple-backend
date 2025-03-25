package com.feple.feple_backend.service;

import com.feple.feple_backend.domain.post.Post;
import com.feple.feple_backend.dto.post.PostRequestDto;
import com.feple.feple_backend.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;

    public Long createPost(PostRequestDto dto){
        Post post = Post.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .boardType(dto.getBoardType())
                .likeCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return postRepository.save(post).getId();
    }
}
