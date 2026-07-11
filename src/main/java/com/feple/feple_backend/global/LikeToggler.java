package com.feple.feple_backend.global;

import java.util.function.IntSupplier;

/**
 * "삭제 시도 → 있었으면 취소(감소), 없었으면 저장(증가)" 좋아요 토글 결정 구조를 한 곳에서 관리한다.
 * 어떤 리포지토리를 쓰는지, 이벤트를 발행하는지, 동시성 가드가 있는지 등 도메인별 세부사항은
 * 각 서비스가 콜백으로 제공한다.
 */
public final class LikeToggler {

    private LikeToggler() {}

    public static boolean toggle(IntSupplier deleteAndCount, Runnable onUnlike, Runnable onLike) {
        if (deleteAndCount.getAsInt() > 0) {
            onUnlike.run();
            return false;
        }
        onLike.run();
        return true;
    }
}
