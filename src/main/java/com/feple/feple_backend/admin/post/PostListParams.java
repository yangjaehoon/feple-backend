package com.feple.feple_backend.admin.post;

record PostListParams(String filter, String keyword, Long artistId, Long festivalId) {

    String toExtraParams() {
        StringBuilder sb = new StringBuilder("filter=").append(filter);
        if (keyword != null && !keyword.isBlank()) sb.append("&keyword=").append(keyword);
        if (artistId != null) sb.append("&artistId=").append(artistId);
        if (festivalId != null) sb.append("&festivalId=").append(festivalId);
        return sb.toString();
    }

    String toRedirectParams(int page) {
        StringBuilder sb = new StringBuilder("filter=").append(filter).append("&page=").append(page);
        if (artistId != null) sb.append("&artistId=").append(artistId);
        if (festivalId != null) sb.append("&festivalId=").append(festivalId);
        return sb.toString();
    }

    String toBackUrl() {
        if (filter.isBlank() && artistId == null && festivalId == null) return "/admin/posts";
        return "/admin/posts?" + toExtraParams();
    }
}
