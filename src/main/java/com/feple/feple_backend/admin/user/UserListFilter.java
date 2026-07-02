package com.feple.feple_backend.admin.user;

record UserListFilter(String filter, String sort, Integer page, String keyword) {
    UserListFilter {
        filter  = filter  == null ? ""       : filter;
        sort    = sort    == null ? "latest" : sort;
        keyword = keyword == null ? ""       : keyword;
        // page 파라미터 없이 접근 시 null → primitive int 변환 실패(400) 방지
        page    = page == null ? 0 : page;
    }
}
