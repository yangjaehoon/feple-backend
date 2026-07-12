package com.feple.feple_backend.admin.post;

record PostListParams(Integer page, String filter, String keyword, Long artistId, Long festivalId) {

    PostListParams {
        // page/filter 파라미터 없이 접근 시 null → primitive 변환 실패(400)·"filter=null" 방지
        page = page == null ? 0 : page;
        filter = filter == null ? "" : filter;
        keyword = keyword == null ? "" : keyword;
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
