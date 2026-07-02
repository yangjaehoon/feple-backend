package com.feple.feple_backend.admin;

public final class AdminConstants {
    private AdminConstants() {}

    public static final int LIST_PAGE_SIZE         = 20;
    public static final int LOG_PAGE_SIZE          = 50;
    public static final int DASHBOARD_PREVIEW_SIZE = 5;
    public static final int STATS_RECENT_DAYS      = 7;
    public static final int MAX_EXPORT_ROWS        = 50_000;

    public static final String REPORT_TYPE_POST    = "post";
    public static final String REPORT_TYPE_COMMENT = "comment";
    public static final String STATUS_PENDING      = "PENDING";
    public static final String STATUS_ALL          = "ALL";
}
