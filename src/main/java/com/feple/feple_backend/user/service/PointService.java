package com.feple.feple_backend.user.service;

import com.feple.feple_backend.comment.event.CommentCreatedEvent;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.global.JpqlLikeEscaper;
import com.feple.feple_backend.global.PageableFactory;
import com.feple.feple_backend.post.event.PostCreatedEvent;
import com.feple.feple_backend.post.event.PostDeletedByAdminEvent;
import com.feple.feple_backend.post.event.PostLikedEvent;
import com.feple.feple_backend.user.dto.PointLogResponseDto;
import com.feple.feple_backend.user.entity.PointEntry;
import com.feple.feple_backend.user.entity.PointReason;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.UserPointLog;
import com.feple.feple_backend.user.event.AdminPointGrantedEvent;
import com.feple.feple_backend.user.repository.UserPointLogRepository;
import com.feple.feple_backend.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    private static final int MAX_ADMIN_REASON_LENGTH = 100;

    private final UserRepository userRepository;
    private final UserPointLogRepository pointLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void addPoint(Long userId, PointEntry entry) {
        if (!userRepository.existsById(userId)) return;
        // 원자적 UPDATE로 point를 갱신 — 동시 이벤트 간 lost update 방지(User.point 참조).
        userRepository.addPointAtomically(userId, entry.delta());
        User user = userRepository.getReferenceById(userId);
        pointLogRepository.save(UserPointLog.of(user, entry));
    }

    @Transactional
    public void addCertApprovedPoint(Long userId, Long certId) {
        addPoint(userId, new PointEntry(POINT_CERT_APPROVED, PointReason.CERT_APPROVED, certId));
    }

    /** 관리자 회원 상세 페이지의 "최근 포인트 내역" 카드 전용 */
    @Transactional(readOnly = true)
    public List<PointLogResponseDto> getRecentPointLogs(Long userId, int limit) {
        return pointLogRepository.findByUserId(userId, PageRequest.of(0, limit))
                .getContent().stream()
                .map(PointLogResponseDto::from)
                .toList();
    }

    /** 관리자 "포인트 내역" 목록 페이지 전용 — 전체 유저 대상 페이지네이션 조회 */
    @Transactional(readOnly = true)
    public Page<PointLogResponseDto> getAllPointLogs(int page, int size, String keyword) {
        String escaped = JpqlLikeEscaper.escapeOrNull(keyword);
        Page<UserPointLog> logs = escaped == null
                ? pointLogRepository.findAllWithUser(PageableFactory.orderByLatestFirst(page, size))
                : pointLogRepository.searchByUserKeyword(escaped, PageableFactory.orderByLatestFirst(page, size));
        return logs.map(PointLogResponseDto::from);
    }

    /** 관리자가 사유를 입력해 직접 포인트를 지급/차감 — 지급 후 대상 유저에게 알림 발송 */
    @Transactional
    public void grantByAdmin(Long userId, int amount, String reason) {
        if (amount == 0) {
            throw new IllegalArgumentException("지급할 포인트는 0이 될 수 없습니다.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("지급 사유를 입력해주세요.");
        }
        if (reason.length() > MAX_ADMIN_REASON_LENGTH) {
            throw new IllegalArgumentException("지급 사유는 " + MAX_ADMIN_REASON_LENGTH + "자 이내로 입력해주세요.");
        }
        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");
        userRepository.addPointAtomically(userId, amount);
        pointLogRepository.save(UserPointLog.ofAdminGrant(user, amount, reason));
        eventPublisher.publishEvent(new AdminPointGrantedEvent(userId, amount, reason));
    }

    // @Async로 별도 스레드에서 실행돼 원 트랜잭션 컨텍스트가 없음 — REQUIRES_NEW로 새 트랜잭션 시작
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPostCreated(PostCreatedEvent event) {
        addPoint(event.authorId(), new PointEntry(POINT_POST_CREATED, PointReason.POST_CREATED, event.postId()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCommentCreated(CommentCreatedEvent event) {
        if (event.commenterId() == null) return;
        addPoint(event.commenterId(), new PointEntry(POINT_COMMENT_CREATED, PointReason.COMMENT_CREATED, event.postId()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPostLiked(PostLikedEvent event) {
        addPoint(event.postAuthorId(), new PointEntry(POINT_POST_LIKED_RECEIVED, PointReason.POST_LIKED_RECEIVED, event.postId()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPostDeletedByAdmin(PostDeletedByAdminEvent event) {
        addPoint(event.postAuthorId(), new PointEntry(POINT_POST_DELETED_BY_ADMIN, PointReason.POST_DELETED_BY_ADMIN, null));
    }
}
