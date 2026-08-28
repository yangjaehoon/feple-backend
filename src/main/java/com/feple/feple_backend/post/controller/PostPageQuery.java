package com.feple.feple_backend.post.controller;

import com.feple.feple_backend.global.PageSize;
import com.feple.feple_backend.post.dto.CursorPageRequest;

/**
 * 게시글 목록 엔드포인트 공통 쿼리 파라미터(cursor / size / sort).
 * size는 [1, MAX_PAGE_SIZE]로 클램프하고, 없으면 기본값을 적용한다.
 * sort를 받지 않는 목록(태그·아티스트·페스티벌별)에서는 sort가 무시된다.
 */
record PostPageQuery(Long cursor, Integer size, String sort) {

    static final String SORT_POPULAR = "popular";
    static final String SORT_LATEST = "latest";

    PostPageQuery {
        // size=0·음수면 PageRequest.of()가 예외를 던지므로 하한도 함께 건다
        size = (size == null)
                ? PageSize.DEFAULT_PAGE_SIZE
                : Math.max(1, Math.min(size, PageSize.MAX_PAGE_SIZE));
        sort = (sort == null || sort.isBlank()) ? SORT_LATEST : sort;
    }

    boolean isPopular() {
        return SORT_POPULAR.equals(sort);
    }

    CursorPageRequest toPageRequest(Long viewerId) {
        return new CursorPageRequest(cursor, size, viewerId);
    }
}
