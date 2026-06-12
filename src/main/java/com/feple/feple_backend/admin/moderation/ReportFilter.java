package com.feple.feple_backend.admin.moderation;

record ReportFilter(String type, String status, int page, String keyword) {
    ReportFilter {
        type    = (type    == null || type.isBlank())    ? "post"    : type;
        status  = (status  == null || status.isBlank())  ? "PENDING" : status;
        keyword = keyword == null ? "" : keyword;
    }
}
