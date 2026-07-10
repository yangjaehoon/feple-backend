package com.feple.feple_backend.admin.certification;

record CertificationFilter(String status, int page, String keyword) {
    CertificationFilter {
        status  = status  == null ? "" : status;
        keyword = keyword == null ? "" : keyword;
    }
}
