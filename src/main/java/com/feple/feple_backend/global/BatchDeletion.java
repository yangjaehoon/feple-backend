package com.feple.feple_backend.global;

import java.util.function.IntSupplier;

/**
 * 오래된 로그·알림을 커넥션을 오래 붙잡지 않도록 배치로 나눠 삭제하는 공통 루프.
 * 알림·접속 로그·검색 로그 정리 스케줄러가 동일하게 사용한다.
 */
public final class BatchDeletion {

    private BatchDeletion() {}

    /** 한 배치에서 삭제할 최대 행 수 — 배치 삭제 쿼리의 LIMIT과 반드시 일치시킨다. */
    public static final int BATCH_SIZE = 1000;

    /**
     * {@code deleteBatch}가 {@link #BATCH_SIZE} 미만을 반환할 때까지(= 더 지울 행이 없을 때까지)
     * 반복 호출한다. 각 호출이 독립 트랜잭션으로 커밋되도록 배치 삭제 쿼리에 맡긴다.
     *
     * @param deleteBatch 한 배치를 삭제하고 삭제된 행 수를 반환하는 연산
     * @return 총 삭제 행 수
     */
    public static int repeatUntilExhausted(IntSupplier deleteBatch) {
        int total = 0;
        int deleted;
        do {
            deleted = deleteBatch.getAsInt();
            total += deleted;
        } while (deleted == BATCH_SIZE);
        return total;
    }
}
