package com.feple.feple_backend.admin.festival;

import com.feple.feple_backend.admin.AdminParamDefaults;
import com.feple.feple_backend.admin.AdminUrlUtils;

record FestivalListParams(Integer page, String keyword) {

    FestivalListParams {
        page = Math.max(0, AdminParamDefaults.orZero(page));
        keyword = AdminParamDefaults.orEmpty(keyword);
    }

    /** 상세 화면의 "목록으로" 링크(returnUrl)에 쓰는 인코딩된 URL. */
    String toListUrl() {
        return AdminUrlUtils.listUrl("/admin/festivals", "page", page, "keyword", keyword);
    }
}
