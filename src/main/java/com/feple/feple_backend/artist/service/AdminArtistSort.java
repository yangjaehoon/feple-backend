package com.feple.feple_backend.artist.service;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import java.util.Comparator;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

// 관리자 아티스트 목록 정렬 키 — DB 레벨 정렬(dbSort)과 곡수 기준 인메모리 정렬(inMemoryComparator)이
// 여러 메서드에 문자열 리터럴로 흩어져 있던 것을 하나의 타입으로 묶는다.
enum AdminArtistSort {
    NAME(Sort.by(Direction.ASC, "name"), null),
    NAME_DESC(Sort.by(Direction.DESC, "name"), null),
    FOLLOWERS(Sort.by(Direction.DESC, "followerCount"), null),
    FOLLOWERS_ASC(Sort.by(Direction.ASC, "followerCount"), null),
    SONGS(null, Comparator.comparingInt(ArtistResponseDto::getSongCount).reversed()),
    SONGS_ASC(null, Comparator.comparingInt(ArtistResponseDto::getSongCount)),
    RANKING(Sort.by(Direction.DESC, "weeklyScore").and(Sort.by(Direction.ASC, "id")), null);

    private final Sort dbSort;
    private final Comparator<ArtistResponseDto> inMemoryComparator;

    AdminArtistSort(Sort dbSort, Comparator<ArtistResponseDto> inMemoryComparator) {
        this.dbSort = dbSort;
        this.inMemoryComparator = inMemoryComparator;
    }

    static AdminArtistSort from(String raw) {
        if (raw == null || raw.isBlank()) return RANKING;
        try {
            return valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return RANKING;
        }
    }

    Sort dbSort() {
        return dbSort;
    }

    boolean requiresInMemorySort() {
        return inMemoryComparator != null;
    }

    Comparator<ArtistResponseDto> inMemoryComparator() {
        return inMemoryComparator;
    }
}
