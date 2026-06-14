package com.feple.feple_backend.festival.entity;

public enum EventType {
    FESTIVAL("페스티벌"),
    FAN_MEETING("팬미팅"),
    TV_SHOW("TV 출연");

    private final String displayName;

    EventType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
