package com.feple.feple_backend.admin.post;

import com.feple.feple_backend.admin.AdminParamDefaults;

record PostListParams(Integer page, String filter, String keyword, Long artistId, Long festivalId) {

    PostListParams {
        // filter 파라미터 없이 접근 시 "filter=null" 방지
        page = AdminParamDefaults.orZero(page);
        filter = AdminParamDefaults.orEmpty(filter);
        keyword = AdminParamDefaults.orEmpty(keyword);
    }

    String toExtraParams() {
        StringBuilder sb = new StringBuilder("filter=").append(filter);
        if (!keyword.isBlank()) sb.append("&keyword=").append(keyword);
        if (artistId != null) sb.append("&artistId=").append(artistId);
        if (festivalId != null) sb.append("&festivalId=").append(festivalId);
        return sb.toString();
    }

    String toRedirectParams() {
        StringBuilder sb = new StringBuilder("filter=").append(filter).append("&page=").append(page);
        if (artistId != null) sb.append("&artistId=").append(artistId);
        if (festivalId != null) sb.append("&festivalId=").append(festivalId);
        return sb.toString();
    }

}
