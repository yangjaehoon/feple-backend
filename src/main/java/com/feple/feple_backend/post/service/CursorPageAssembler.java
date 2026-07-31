package com.feple.feple_backend.post.service;

import com.feple.feple_backend.post.dto.CursorPage;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.PageRequest;

/**
 * cursor 기반 페이지네이션 공통 산출 로직: size+1 fetch → hasNext 판정 → size만큼 자르기 → nextCursor 계산.
 * nextCursor는 필터링 전 raw 목록(pageItems) 기준으로 계산해야 한다 — 차단된 작성자의 글이
 * 이번 배치를 전부 채워 content가 비어도(hasNext=true) 다음 배치를 계속 조회할 수 있어야 한다.
 */
final class CursorPageAssembler {
    private CursorPageAssembler() {}

    static <E, D> CursorPage<D> assemble(Long cursor, int size,
                                          Function<PageRequest, List<E>> fetchFirst,
                                          Function<PageRequest, List<E>> fetchAfterCursor,
                                          Function<List<E>, List<D>> contentBuilder,
                                          Function<E, Long> idExtractor) {
        int fetchSize = size + 1;
        PageRequest limit = PageRequest.of(0, fetchSize);
        List<E> items = (cursor == null) ? fetchFirst.apply(limit) : fetchAfterCursor.apply(limit);
        boolean hasNext = items.size() == fetchSize;
        List<E> pageItems = items.stream().limit(size).toList();
        List<D> content = contentBuilder.apply(pageItems);
        Long nextCursor = hasNext && !pageItems.isEmpty() ? idExtractor.apply(pageItems.get(pageItems.size() - 1)) : null;
        return new CursorPage<>(content, nextCursor, hasNext);
    }
}
