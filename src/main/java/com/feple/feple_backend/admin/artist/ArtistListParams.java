package com.feple.feple_backend.admin.artist;

import com.feple.feple_backend.admin.support.AdminParamDefaults;
import com.feple.feple_backend.admin.support.AdminUrlUtils;
import com.feple.feple_backend.global.MusicGenre;

record ArtistListParams(Integer page, String keyword, String sort, MusicGenre genre) {

    ArtistListParams {
        page = Math.max(0, AdminParamDefaults.orZero(page));
        keyword = AdminParamDefaults.orEmpty(keyword);
        sort = AdminParamDefaults.orEmpty(sort);
    }

    /** 편집 후 돌아갈 목록 URL. 필터/정렬/장르 상태를 그대로 보존한다(빈 값은 생략). */
    String toListUrl() {
        return AdminUrlUtils.listUrl("/admin/artists",
                "page", page, "keyword", keyword, "sort", sort, "genre", genre);
    }
}
