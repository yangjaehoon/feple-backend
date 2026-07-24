package com.feple.feple_backend.global;

import org.springframework.dao.DataIntegrityViolationException;

import java.util.function.IntSupplier;

/**
 * "삭제 시도 → 있었으면 취소(감소), 없었으면 저장(증가)" 좋아요 토글 결정 구조를 한 곳에서 관리한다.
 * 어떤 리포지토리를 쓰는지, 이벤트를 발행하는지 등 도메인별 세부사항은 각 서비스가 콜백으로 제공한다.
 */
public final class LikeToggler {

    private LikeToggler() {}

    public static boolean toggle(IntSupplier deleteAndCount, Runnable onUnlike, Runnable onLike) {
        if (deleteAndCount.getAsInt() > 0) {
            onUnlike.run();
            return false;
        }
        try {
            onLike.run();
        } catch (DataIntegrityViolationException ignored) {
            // unique(user_id, 대상_id) 제약 위반: 동시 요청 경합으로 이미 다른 요청이 저장/카운트
            // 증가를 마쳤다는 뜻이라 정상 흐름이다. onLike는 반드시 saveAndFlush를 사용해야
            // 이 시점에 제약 위반이 즉시 드러난다(지연 flush면 여기서 못 잡고 커밋 시점에 터진다).
        }
        return true;
    }
}
