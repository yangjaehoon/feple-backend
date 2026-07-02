package com.feple.feple_backend.admin.moderation;

import com.feple.feple_backend.admin.AdminConstants;

record ReportFilter(String type, String status, Integer page, String keyword) {
    ReportFilter {
        type    = (type    == null || type.isBlank())    ? AdminConstants.REPORT_TYPE_POST : type;
        status  = (status  == null || status.isBlank())  ? AdminConstants.STATUS_PENDING   : status;
        keyword = keyword == null ? "" : keyword;
        // page 파라미터 없이 접근 시 null → primitive int 변환 실패(400) 방지
        page    = page == null ? 0 : page;
    }
}
