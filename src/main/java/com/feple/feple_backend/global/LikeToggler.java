package com.feple.feple_backend.global;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import org.springframework.dao.DataIntegrityViolationException;

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

    /**
     * {@link #toggle}와 동일한 토글 결정 구조지만, 좋아요 <b>추가</b> 여부를 "매칭 없는 DELETE"가
     * 아니라 락을 잡지 않는 존재 조회({@code exists})로 판별한다. 추가 경로가 순수 INSERT라
     * 빈 인덱스 구간에 갭 락을 잡지 않으므로, 같은 유저가 여러 대상을 동시에 좋아요할 때
     * (예: 온보딩 페스티벌 선택) InnoDB 갭 락 + insert-intention 락 데드락을 피한다.
     *
     * <p>취소 판단은 {@link #toggle}과 동일하게 {@code deleteAndCount}의 반환값(&gt;0)으로 하며,
     * 그때만 {@code onUnlike}(카운터 감소 등)를 실행한다 — 동시 취소 경합으로 삭제 0건이면
     * 카운터를 건드리지 않는다. 조회~저장 사이 다른 요청이 먼저 저장하는 TOCTOU는 {@code onLike}가
     * 던지는 {@link DataIntegrityViolationException}을 삼켜 처리하므로, {@code onLike}는 반드시
     * saveAndFlush를 사용해야 이 시점에 제약 위반이 즉시 드러난다.
     */
    public static boolean toggleByExistence(
            BooleanSupplier exists, IntSupplier deleteAndCount, Runnable onUnlike, Runnable onLike) {
        if (exists.getAsBoolean()) {
            if (deleteAndCount.getAsInt() > 0) {
                onUnlike.run();
            }
            return false;
        }
        try {
            onLike.run();
        } catch (DataIntegrityViolationException ignored) {
            // 동시 요청이 이미 저장/카운트 증가를 마침 — 정상 흐름
        }
        return true;
    }
}
