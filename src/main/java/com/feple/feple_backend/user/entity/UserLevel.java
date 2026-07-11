package com.feple.feple_backend.user.entity;

public enum UserLevel {
    SEED(0, "씨앗"),
    SPROUT(100, "새싹"),
    BLOOM(300, "꽃"),
    FESTIVAL(700, "페스티버"),
    LEGEND(1500, "레전드");

    public final int requiredMinPoint;
    public final String displayName;

    UserLevel(int requiredMinPoint, String displayName) {
        this.requiredMinPoint = requiredMinPoint;
        this.displayName = displayName;
    }

    public static UserLevel of(int point) {
        UserLevel[] values = values();
        for (int i = values.length - 1; i >= 0; i--) {
            if (point >= values[i].requiredMinPoint) return values[i];
        }
        return SEED;
    }
}
