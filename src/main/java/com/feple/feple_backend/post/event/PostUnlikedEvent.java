package com.feple.feple_backend.post.event;

/**
 * 좋아요 취소 — {@link PostLikedEvent}로 지급한 포인트를 되돌리기 위한 대칭 이벤트.
 *
 * <p>취소 이벤트가 없으면 "좋아요 → 취소 → 좋아요"를 반복하는 것만으로 글쓴이에게 포인트를
 * 무제한 적립할 수 있다(쓰기 한도 분당 30회 기준 약 50분이면 최고 등급 도달).
 * 알림은 보내지 않는다 — 취소는 글쓴이에게 알릴 일이 아니다.
 */
public record PostUnlikedEvent(
        Long postAuthorId,
        Long postId,
        Long unlikerId
) {}
