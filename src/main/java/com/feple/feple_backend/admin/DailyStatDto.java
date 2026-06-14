package com.feple.feple_backend.admin;

public record DailyStatDto(String date, long signups, long posts, long comments, long reports) {}
