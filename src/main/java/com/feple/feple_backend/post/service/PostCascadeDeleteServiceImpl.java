package com.feple.feple_backend.post.service;

import com.feple.feple_backend.post.repository.PostDraftRepository;
import com.feple.feple_backend.post.repository.PostLikeRepository;
import com.feple.feple_backend.post.repository.PostReportRepository;
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
    private final PostDraftRepository postDraftRepository;
    private final PostReportRepository postReportRepository;

    @Override
    @Transactional
    public void removePostActivityByUser(Long userId) {
        postLikeRepository.decrementPostLikeCountByUserId(userId);
        postLikeRepository.deleteByUserId(userId);
        postScrapRepository.decrementPostScrapCountByUserId(userId);
        postScrapRepository.deleteByUserId(userId);
    }

    @Override
    @Transactional
    public void removeAuthoredArtifactsByUser(Long userId) {
        postDraftRepository.deleteByUserId(userId);
        postReportRepository.deleteByReporterId(userId);
    }
}
