package com.feple.feple_backend.post.service;

import com.feple.feple_backend.comment.service.CommentService;
import com.feple.feple_backend.notification.service.NotificationQueryService;
import com.feple.feple_backend.post.repository.PostDraftRepository;
import com.feple.feple_backend.post.repository.PostLikeRepository;
import com.feple.feple_backend.post.repository.PostReportRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.post.repository.PostScrapRepository;
import java.util.List;
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
    private final PostRepository postRepository;
    private final PostDeleter postDeleter;
    private final CommentService commentService;
    private final NotificationQueryService notificationQueryService;

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

    @Override
    @Transactional
    public void purgeAuthoredPostsByUser(Long userId) {
        List<Long> postIds = postRepository.findIdsByUserId(userId);
        if (postIds.isEmpty()) return;
        // 다른 도메인 자식은 해당 도메인 서비스에 위임, post 자기 도메인 자식은 PostDeleter가 처리.
        commentService.deleteByPostIds(postIds);
        notificationQueryService.deleteByPostIds(postIds);
        postDeleter.deleteByIds(postIds);
    }
}
