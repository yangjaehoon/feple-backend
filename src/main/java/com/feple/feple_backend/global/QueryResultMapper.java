package com.feple.feple_backend.global;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QueryResultMapper {

    private QueryResultMapper() {}

    public static Map<Long, Long> toLongMap(List<Object[]> rows) {
        return rows.stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }

    public static Map<Long, Integer> toIntMap(List<Object[]> rows) {
        return rows.stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> ((Long) row[1]).intValue()));
    }

    public static long extractSingleCount(List<Object[]> rows) {
        return rows.isEmpty() ? 0L : (Long) rows.get(0)[1];
    }
}
