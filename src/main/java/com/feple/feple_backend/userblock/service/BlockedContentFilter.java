package com.feple.feple_backend.userblock.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToLongFunction;

/** 피드·댓글 목록에서 조회자가 차단한 작성자의 컨텐츠를 제외한다. */
@Component
@RequiredArgsConstructor
public class BlockedContentFilter {

    private final UserBlockService userBlockService;

    public <T> List<T> excludeBlocked(List<T> items, Long viewerId, ToLongFunction<T> authorIdExtractor) {
        if (viewerId == null || items.isEmpty()) return items;
        Set<Long> blockedIds = new HashSet<>(userBlockService.getBlockedIds(viewerId));
        if (blockedIds.isEmpty()) return items;
        return items.stream()
                .filter(item -> !blockedIds.contains(authorIdExtractor.applyAsLong(item)))
                .toList();
    }
}
