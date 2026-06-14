package com.feple.feple_backend.post.dto;

public record PostAdminFilter(
        int page,
        int size,
        String filter,
        String keyword,
        Long artistId,
        Long festivalId
) {}
