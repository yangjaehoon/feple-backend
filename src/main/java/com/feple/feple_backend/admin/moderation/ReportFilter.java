package com.feple.feple_backend.admin.moderation;

import com.feple.feple_backend.admin.AdminConstants;

record ReportFilter(String type, String status, int page, String keyword) {
    ReportFilter {
        type    = (type    == null || type.isBlank())    ? AdminConstants.REPORT_TYPE_POST : type;
        status  = (status  == null || status.isBlank())  ? AdminConstants.STATUS_PENDING   : status;
        keyword = keyword == null ? "" : keyword;
    }
}
