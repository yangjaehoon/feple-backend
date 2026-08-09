package com.feple.feple_backend.festival.setlistchangerequest.service;

public record SetlistChangeRequestCommand(
        Long userId,
        Long festivalId,
        Long artistFestivalId,
        String message
) {}
