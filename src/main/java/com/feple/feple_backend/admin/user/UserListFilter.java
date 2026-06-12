package com.feple.feple_backend.admin.user;

record UserListFilter(String filter, String sort, int page, String keyword) {
    UserListFilter {
        filter  = filter  == null ? ""       : filter;
        sort    = sort    == null ? "latest" : sort;
        keyword = keyword == null ? ""       : keyword;
    }
}
