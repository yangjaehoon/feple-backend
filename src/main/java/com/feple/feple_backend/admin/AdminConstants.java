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
    public static final int TREND_TOP_LIMIT         = 10;
    public static final int TREND_UPCOMING_DAYS     = 30;
    public static final int BROADCAST_HISTORY_LIMIT = 100;

    public static final String REPORT_TYPE_POST    = "post";
    public static final String REPORT_TYPE_COMMENT = "comment";
    public static final String STATUS_PENDING      = "PENDING";
    public static final String STATUS_ALL          = "ALL";
}
