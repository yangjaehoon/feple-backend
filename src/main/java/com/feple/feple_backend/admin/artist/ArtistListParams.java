package com.feple.feple_backend.admin.artist;

import com.feple.feple_backend.admin.AdminParamDefaults;
import com.feple.feple_backend.admin.AdminUrlUtils;
import org.springframework.web.util.UriComponentsBuilder;

record ArtistListParams(Integer page, String keyword, String sort) {

    ArtistListParams {
        page = AdminParamDefaults.orZero(page);
        keyword = AdminParamDefaults.orEmpty(keyword);
        sort = AdminParamDefaults.orEmpty(sort);
    }

    String toRedirectUrl() {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/artists").queryParam("page", page);
        AdminUrlUtils.appendIfPresent(builder, "keyword", keyword);
        AdminUrlUtils.appendIfPresent(builder, "sort", sort);
        return AdminUrlUtils.encoded(builder);
    }
}
