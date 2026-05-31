package com.feple.feple_backend.festival.entity;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.reverseOrder;

/**
 * 페스티벌 상태 — 각 상태가 필터 조건과 정렬 기준을 직접 소유 (State Pattern)
 */
public enum FestivalStatus {

    ACTIVE {
        @Override
        public Predicate<Festival> predicate(LocalDate today) {
            return f -> f.getEndDate() == null || !f.getEndDate().isBefore(today);
        }

        @Override
        public Comparator<Festival> comparator() {
            return Comparator.comparing(Festival::getStartDate, Comparator.nullsLast(naturalOrder()));
        }
    },

    ENDED {
        @Override
        public Predicate<Festival> predicate(LocalDate today) {
            return f -> f.getEndDate() != null && f.getEndDate().isBefore(today);
        }

        @Override
        public Comparator<Festival> comparator() {
            return Comparator.comparing(Festival::getStartDate, Comparator.nullsLast(reverseOrder()));
        }
    };

    public abstract Predicate<Festival> predicate(LocalDate today);

    public abstract Comparator<Festival> comparator();

    public List<Festival> filter(List<Festival> festivals, LocalDate today) {
        return festivals.stream()
                .filter(predicate(today))
                .sorted(comparator())
                .toList();
    }
}
