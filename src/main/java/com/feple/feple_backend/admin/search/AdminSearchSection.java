package com.feple.feple_backend.admin.search;

import java.util.List;

public record AdminSearchSection<T>(List<T> items, long total, boolean permitted) {

    public static <T> AdminSearchSection<T> notPermitted() {
        return new AdminSearchSection<>(List.of(), 0, false);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
