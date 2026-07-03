package com.feple.feple_backend.post.service;

import com.feple.feple_backend.global.EntityFinder;
import com.feple.feple_backend.global.PageSize;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.entity.PostScrap;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.post.repository.PostScrapRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        Post post = EntityFinder.getOrThrow(postRepository::findById, postId, "게시글");
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");

        int deleted = postScrapRepository.deleteByUserIdAndPostId(userId, postId);
        if (deleted > 0) {
            postRepository.decrementScrapCount(postId);
            return false;
        }
        postScrapRepository.save(new PostScrap(user, post));
        postRepository.incrementScrapCount(postId);
        return true;
    }

    public long countMyScraps(Long userId) {
        return postScrapRepository.countByUserId(userId);
    }

    /** 내 스크랩 목록 조회 */
    public List<PostResponseDto> getMyScraps(Long userId) {
        return postScrapRepository.findByUserIdOrderByIdDesc(userId, PageRequest.of(0, PageSize.MY_ACTIVITIES))
                .stream()
                .map(scrap -> PostResponseDto.from(scrap.getPost()))
                .toList();
    }
}
