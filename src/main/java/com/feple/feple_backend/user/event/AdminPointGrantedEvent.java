package com.feple.feple_backend.user.event;

public record AdminPointGrantedEvent(Long userId, int amount, String reason) {}
