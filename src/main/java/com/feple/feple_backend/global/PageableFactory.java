package com.feple.feple_backend.global;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageableFactory {
    private PageableFactory() {}

    public static Pageable orderByLatestFirst(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public static Pageable orderByLatestId(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
    }

    public static Pageable orderByLatestStartDate(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startDate"));
    }
}
