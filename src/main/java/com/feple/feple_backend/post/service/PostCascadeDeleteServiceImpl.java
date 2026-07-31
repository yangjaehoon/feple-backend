package com.feple.feple_backend.post.service;

import com.feple.feple_backend.post.repository.PostLikeRepository;
import com.feple.feple_backend.post.repository.PostScrapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostCascadeDeleteServiceImpl implements PostCascadeDeleteService {

    private final PostLikeRepository postLikeRepository;
    private final PostScrapRepository postScrapRepository;

    @Override
    @Transactional
    public void removePostActivityByUser(Long userId) {
        postLikeRepository.decrementPostLikeCountByUserId(userId);
        postLikeRepository.deleteByUserId(userId);
        postScrapRepository.decrementPostScrapCountByUserId(userId);
        postScrapRepository.deleteByUserId(userId);
    }
}
