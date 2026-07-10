package com.feple.feple_backend.admin.certification;

record CertFilter(String status, int page, String keyword) {
    CertFilter {
        status  = status  == null ? "" : status;
        keyword = keyword == null ? "" : keyword;
    }
}
