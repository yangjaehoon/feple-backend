package com.feple.feple_backend.post.dto;

public record PostAdminFilterDto(
        int page,
        int size,
        String filter,
        String keyword,
        Long artistId,
        Long festivalId
) {}
