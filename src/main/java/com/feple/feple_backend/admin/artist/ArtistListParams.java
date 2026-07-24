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
        return builder.build().toUriString();
    }
}
