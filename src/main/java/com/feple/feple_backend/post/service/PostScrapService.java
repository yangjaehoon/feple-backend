package com.feple.feple_backend.post.service;

import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.entity.PostScrap;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.post.repository.PostScrapRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostScrapService {

    private final PostScrapRepository postScrapRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    /** 특정 게시글 스크랩 여부 확인 */
    public boolean isScrapedByUser(Long postId, Long userId) {
        if (userId == null) return false;
        return postScrapRepository.existsByUserIdAndPostId(userId, postId);
    }

    /** 스크랩 토글 — 현재 스크랩 상태 반환 */
    @Transactional
    public boolean toggleScrap(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다: " + postId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다: " + userId));

        Optional<PostScrap> existing = postScrapRepository.findByUserIdAndPostId(userId, postId);
        if (existing.isPresent()) {
            postScrapRepository.delete(existing.get());
            return false;
        } else {
            postScrapRepository.save(PostScrap.builder().user(user).post(post).build());
            return true;
        }
    }

    /** 내 스크랩 목록 조회 */
    public List<PostResponseDto> getMyScraps(Long userId) {
        return postScrapRepository.findByUserIdOrderByIdDesc(userId)
                .stream()
                .map(scrap -> PostResponseDto.from(scrap.getPost()))
                .toList();
    }
}
