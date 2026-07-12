package com.feple.feple_backend.admin.certification;

record CertificationFilter(String status, Integer page, String keyword) {
    CertificationFilter {
        status  = status  == null ? "" : status;
        // page 파라미터 없이 접근 시 null → primitive 변환 실패(400) 방지
        page    = page    == null ? 0  : page;
        keyword = keyword == null ? "" : keyword;
    }
}
