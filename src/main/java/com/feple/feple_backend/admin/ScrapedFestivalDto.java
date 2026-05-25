package com.feple.feple_backend.admin;

public record ScrapedFestivalDto(
    String title,
    String description,
    String location,
    String startDate,
    String endDate,
    String posterImageUrl,
    String sourceUrl,
    String source,
    String warning
) {}
