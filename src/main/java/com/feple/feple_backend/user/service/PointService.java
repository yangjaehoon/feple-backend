package com.feple.feple_backend.user.service;

import com.feple.feple_backend.comment.event.CommentCreatedEvent;
import com.feple.feple_backend.post.event.PostCreatedEvent;
import com.feple.feple_backend.post.event.PostDeletedByAdminEvent;
import com.feple.feple_backend.post.event.PostLikedEvent;
import com.feple.feple_backend.user.entity.PointReason;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.UserPointLog;
import com.feple.feple_backend.user.repository.UserPointLogRepository;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
public class PointService {

    private static final int POINT_POST_CREATED = 5;
    private static final int POINT_COMMENT_CREATED = 2;
    private static final int POINT_POST_LIKED_RECEIVED = 1;
    private static final int POINT_POST_DELETED_BY_ADMIN = -5;
    private static final int POINT_CERT_APPROVED = 10;

    private final UserRepository userRepository;
    private final UserPointLogRepository pointLogRepository;

    @Transactional
    public void addPoint(Long userId, int delta, PointReason reason, Long refId) {
        if (!userRepository.existsById(userId)) return;
        // 원자적 UPDATE로 point를 갱신 — 동시 이벤트 간 lost update 방지(User.point 참조).
        userRepository.addPointAtomically(userId, delta);
        User user = userRepository.getReferenceById(userId);
        pointLogRepository.save(UserPointLog.of(user, delta, reason, refId));
    }

    @Transactional
    public void addCertApprovedPoint(Long userId, Long certId) {
        addPoint(userId, POINT_CERT_APPROVED, PointReason.CERT_APPROVED, certId);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPostCreated(PostCreatedEvent event) {
        addPoint(event.authorId(), POINT_POST_CREATED, PointReason.POST_CREATED, event.postId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCommentCreated(CommentCreatedEvent event) {
        if (event.commenterId() == null) return;
        addPoint(event.commenterId(), POINT_COMMENT_CREATED, PointReason.COMMENT_CREATED, event.postId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPostLiked(PostLikedEvent event) {
        addPoint(event.postAuthorId(), POINT_POST_LIKED_RECEIVED, PointReason.POST_LIKED_RECEIVED, event.postId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPostDeletedByAdmin(PostDeletedByAdminEvent event) {
        addPoint(event.postAuthorId(), POINT_POST_DELETED_BY_ADMIN, PointReason.POST_DELETED_BY_ADMIN, null);
    }
}
