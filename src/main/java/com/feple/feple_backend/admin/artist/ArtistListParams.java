package com.feple.feple_backend.admin.artist;

import org.springframework.web.util.UriComponentsBuilder;

record ArtistListParams(Integer page, String keyword, String sort) {

    ArtistListParams {
        page = page == null ? 0 : page;
        keyword = keyword == null ? "" : keyword;
        sort = sort == null ? "" : sort;
    }

    String toRedirectUrl() {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/artists").queryParam("page", page);
        if (!keyword.isBlank()) builder.queryParam("keyword", keyword);
        if (!sort.isBlank()) builder.queryParam("sort", sort);
        return builder.build().toUriString();
    }
}
