package com.feple.feple_backend.admin;

import java.util.List;

public record ScraperApplyRequest(
    String title,
    String titleEn,
    String description,
    String location,
    String startDate,
    String endDate,
    String region,
    List<String> genres
) {}
