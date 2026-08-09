package com.feple.feple_backend.user.entity;

import lombok.Getter;

@Getter
public enum UserLevel {
    SEED(0),
    SPROUT(100),
    BLOOM(300),
    FESTIVAL(700),
    LEGEND(1500);

    private final int requiredMinPoint;

    UserLevel(int requiredMinPoint) {
        this.requiredMinPoint = requiredMinPoint;
    }

    public static UserLevel of(int point) {
        UserLevel[] values = values();
        for (int i = values.length - 1; i >= 0; i--) {
            if (point >= values[i].requiredMinPoint) return values[i];
        }
        return SEED;
    }
}
