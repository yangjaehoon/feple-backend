package com.feple.feple_backend.global;

public final class PageSize {
    private PageSize() {}

    public static final int MAX_PAGE_SIZE = 50;
    public static final int POPULAR_POSTS = 4;
    public static final int POSTS = 100;
    public static final int MY_ACTIVITIES = 200;
    public static final int FESTIVALS = 200;
    // findByFilters()가 상태 분류·정렬을 위해 DB에서 끌어오는 상한 — 최종 노출 개수(FESTIVALS)보다
    // 넉넉히 커서 오늘 수준의 데이터에서는 결과가 절대 달라지지 않으면서도, 테이블이 계속 커져도
    // 요청당 메모리·정렬 비용에 상한을 둔다
    public static final int FESTIVAL_FILTER_FETCH_CAP = 1000;
    public static final int COMMENTS = 500;
    public static final int SEARCH = 10;
}
