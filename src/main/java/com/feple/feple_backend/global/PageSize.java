package com.feple.feple_backend.global;

public final class PageSize {
    private PageSize() {}

    public static final int MAX_PAGE_SIZE = 50;
    public static final int POPULAR_POSTS = 4;
    // 인기글 캐시(PopularPostCache)는 조회자와 무관한 공통 캐시라 차단 필터링을 캐시 내부에서
    // 할 수 없다 — 캐시된 풀이 정확히 POPULAR_POSTS(4)개뿐이면, 그중 일부만 차단 작성자여도
    // 노출 개수가 눈에 띄게 줄어든다(페스티벌 캐러셀 정렬 버그와 같은 유형). 최종 노출 개수보다
    // 넉넉히 캐싱해두고 필터링 후 앞에서 자른다.
    public static final int POPULAR_POSTS_POOL = 20;
    public static final int POSTS = 100;
    public static final int MY_ACTIVITIES = 200;
    public static final int FESTIVALS = 200;
    // findByFilters()가 상태 분류·정렬을 위해 DB에서 끌어오는 상한 — 최종 노출 개수(FESTIVALS)보다
    // 넉넉히 커서 오늘 수준의 데이터에서는 결과가 절대 달라지지 않으면서도, 테이블이 계속 커져도
    // 요청당 메모리·정렬 비용에 상한을 둔다
    public static final int FESTIVAL_FILTER_FETCH_CAP = 1000;
    public static final int COMMENTS = 500;
    public static final int SEARCH = 10;
    // 검색 결과도 차단 필터링 후 노출 — 다음 페이지 개념이 없는 단발성 목록이라 필터링으로
    // 결과가 줄어들어도 재요청으로 보충할 방법이 없다. 넉넉히 조회한 뒤 필터링 후 자른다.
    public static final int SEARCH_POOL = 30;
}
