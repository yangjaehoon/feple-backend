package com.feple.feple_backend.admin.checklist;

import java.util.Arrays;

public enum ChecklistField {
    LINEUP_1("lineup1"),
    LINEUP_2("lineup2"),
    LINEUP_3("lineup3"),
    BOOTH_MAP("boothMap"),
    TIMETABLE("timetable");

    private final String key;

    ChecklistField(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public static ChecklistField fromKey(String key) {
        return Arrays.stream(values())
                .filter(f -> f.key.equals(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("알 수 없는 항목: " + key));
    }
}
