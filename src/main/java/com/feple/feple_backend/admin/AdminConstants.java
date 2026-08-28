package com.feple.feple_backend.admin;

public final class AdminConstants {
    private AdminConstants() {}

    public static final int LIST_PAGE_SIZE          = 20;
    public static final int FESTIVAL_LIST_PAGE_SIZE = 30;
    public static final int LOG_PAGE_SIZE           = 50;
    public static final int DASHBOARD_PREVIEW_SIZE  = 5;
    public static final int SUGGESTION_PREVIEW_SIZE = 50;
    public static final int STATS_RECENT_DAYS       = 7;
    public static final int MAX_EXPORT_ROWS         = 50_000;
    public static final int DELETED_POSTS_LIMIT     = 200;
    public static final int BLINDED_POSTS_LIMIT     = 200;
    public static final int TREND_TOP_LIMIT         = 10;
    public static final int TREND_UPCOMING_DAYS     = 30;
    // 대시보드 "인기 게시글" 트렌드의 집계 대상 기간(일). TREND_UPCOMING_DAYS와 값은 같지만
    // 의미(다가오는 축제 창 vs 게시글 lookback)가 달라 별도 상수로 둔다.
    public static final int TREND_POST_LOOKBACK_DAYS = 30;
    public static final int BROADCAST_HISTORY_LIMIT  = 100;
    public static final int POST_DETAIL_COMMENT_LIMIT = 100;
    public static final int PUSH_TITLE_MAX_LENGTH = 100;
    public static final int PUSH_BODY_MAX_LENGTH  = 500;
    public static final int NICKNAME_SEARCH_RESULT_LIMIT = 20;
    // 신고가 이 건수(대기 상태) 이상 쌓이면 관리자 검토 전이라도 자동으로 블라인드 처리한다.
    public static final int AUTO_BLIND_REPORT_THRESHOLD = 5;

    // 일괄 작업(회원/게시글/신고/인증)에서 한 번에 처리할 수 있는 최대 항목 수.
    // 목록 페이지 크기(최대 30)를 크게 웃도는 값이라 정상 업무에는 제약이 없고, URL/폼 조작으로
    // 수천 건이 단일 트랜잭션에 묶여 장시간 락을 잡는 상황을 차단한다. 감사 로그 detail에
    // 선택 id를 그대로 남겨도 컬럼 길이(2000자)에 여유가 있도록 잡은 상한이기도 하다.
    public static final int BULK_ACTION_MAX_IDS = 50;

    public static final String REPORT_TYPE_POST    = "post";
    public static final String REPORT_TYPE_COMMENT = "comment";
    public static final String REPORT_TYPE_PHOTO   = "photo";
    public static final String REPORT_TYPE_USER    = "user";
    public static final String STATUS_PENDING      = "PENDING";
    public static final String STATUS_ALL          = "ALL";

    public static final String MSG_EMPTY_SELECTION = "선택된 항목이 없습니다.";
    public static final String MSG_BULK_TOO_MANY =
            "한 번에 최대 " + BULK_ACTION_MAX_IDS + "건까지 처리할 수 있습니다. 선택을 줄여 다시 시도해주세요.";
    public static final String MSG_BULK_DELETE_ERROR = "일괄 삭제 처리 중 오류가 발생했습니다.";
    public static final String MSG_DELETE_ERROR = "삭제 중 오류가 발생했습니다.";
    public static final String MSG_PROCESS_ERROR = "처리 중 오류가 발생했습니다.";
    public static final String MSG_UPDATE_ERROR = "수정 중 오류가 발생했습니다.";
    public static final String MSG_RESTORE_ERROR = "복구 중 오류가 발생했습니다.";
}
