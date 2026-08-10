package com.feple.feple_backend.admin.post;

import com.feple.feple_backend.admin.AdminParamDefaults;
import com.feple.feple_backend.admin.AdminUrlUtils;

record PostListParams(Integer page, String filter, String keyword, Long artistId, Long festivalId) {

    PostListParams {
        // filter 파라미터 없이 접근 시 "filter=null" 방지
        page = AdminParamDefaults.orZero(page);
        filter = AdminParamDefaults.orEmpty(filter);
        keyword = AdminParamDefaults.orEmpty(keyword);
    }

    String toExtraParams() {
        return AdminUrlUtils.buildQueryString("filter", filter, "keyword", keyword, "artistId", artistId, "festivalId", festivalId);
    }

    String toRedirectParams() {
        return AdminUrlUtils.buildQueryString("filter", filter, "page", page, "artistId", artistId, "festivalId", festivalId);
    }

}
