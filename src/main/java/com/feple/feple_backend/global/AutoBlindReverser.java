package com.feple.feple_backend.global;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToLongFunction;

/**
 * 신고 반려로 남은 대기 신고가 {@link ReportPolicy#AUTO_BLIND_PENDING_THRESHOLD} 아래로 내려가면
 * 자동 블라인드를 해제하는 판정 로직. PostReportService와 CommentReportService가 대상 타입만 다르고
 * 완전히 동일한 구조를 각자 갖고 있어 임계치 비교가 두 곳에 복제돼 있었다.
 *
 * <p>같은 파일의 {@code submitReport}는 빌더 타입이 달라 통합하지 않는다고 문서화돼 있지만,
 * 이쪽은 리포지토리 메서드 참조만 다르므로 정책을 한 곳으로 모은다.
 */
public final class AutoBlindReverser {

    private AutoBlindReverser() {}

    private static boolean belowThreshold(long pendingCount) {
        return pendingCount < ReportPolicy.AUTO_BLIND_PENDING_THRESHOLD;
    }

    /**
     * 단건 — 대기 신고가 임계치 아래면 블라인드를 해제한다.
     *
     * @param lookup 대상을 찾는 함수. 대상이 이미 하드 삭제됐으면 비어 있을 수 있다.
     */
    public static <T> void unblindIfBelowThreshold(Long targetId,
                                                    ToLongFunction<Long> pendingCounter,
                                                    Function<Long, Optional<T>> lookup,
                                                    Consumer<T> unblind) {
        if (!belowThreshold(pendingCounter.applyAsLong(targetId))) return;
        lookup.apply(targetId).ifPresent(unblind);
    }

    /**
     * 배치 — 일괄 반려(최대 20건)에서 대상마다 조회를 반복하면 최대 40쿼리가 되므로,
     * 그룹 집계 1쿼리 + IN 조회 1쿼리로 묶는다.
     *
     * @param pendingCounts [targetId, count] 형태의 그룹 집계 결과를 반환하는 함수
     */
    public static <T> void unblindAllBelowThreshold(List<Long> targetIds,
                                                     Function<Collection<Long>, List<Object[]>> pendingCounts,
                                                     Function<List<Long>, List<T>> lookupAll,
                                                     Consumer<T> unblind) {
        if (targetIds.isEmpty()) return;
        Map<Long, Long> countsByTargetId = QueryResultMapper.toLongMap(pendingCounts.apply(targetIds));
        List<Long> toUnblind = targetIds.stream()
                .filter(id -> belowThreshold(countsByTargetId.getOrDefault(id, 0L)))
                .toList();
        if (toUnblind.isEmpty()) return;
        lookupAll.apply(toUnblind).forEach(unblind);
    }
}
