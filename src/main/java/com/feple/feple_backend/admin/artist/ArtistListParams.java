package com.feple.feple_backend.admin.artist;

import com.feple.feple_backend.admin.AdminParamDefaults;
import org.springframework.web.util.UriComponentsBuilder;

record ArtistListParams(Integer page, String keyword, String sort) {

    ArtistListParams {
        page = AdminParamDefaults.orZero(page);
        keyword = AdminParamDefaults.orEmpty(keyword);
        sort = AdminParamDefaults.orEmpty(sort);
    }

    String toRedirectUrl() {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/artists").queryParam("page", page);
        if (!keyword.isBlank()) builder.queryParam("keyword", keyword);
        if (!sort.isBlank()) builder.queryParam("sort", sort);
        // encode() 없이 build()만 하면 keyword의 한글이 그대로 Location 헤더에 들어가
        // Tomcat이 "invalid header"로 판단해 리다이렉트 자체를 제거해버린다(빈 화면 원인).
        return builder.build().encode().toUriString();
    }
}
