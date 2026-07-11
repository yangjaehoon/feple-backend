package com.feple.feple_backend.global;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;

public final class EntityFinder {
    private EntityFinder() {}

    public static <T, ID> T getOrThrow(Function<ID, Optional<T>> finder, ID id, String entityName) {
        return finder.apply(id)
                .orElseThrow(() -> new NoSuchElementException(entityName + "을(를) 찾을 수 없습니다: " + id));
    }
}
