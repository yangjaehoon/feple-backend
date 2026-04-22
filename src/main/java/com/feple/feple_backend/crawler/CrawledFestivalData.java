package com.feple.feple_backend.crawler;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class CrawledFestivalData {
    private String title;
    private String description;
    private String location;
    private LocalDate startDate;
    private LocalDate endDate;
    private String posterImageUrl; // 외부 이미지 URL (S3 아님)
    private String sourceUrl;      // 원본 페이지 URL
    private String sourceSite;     // 사이트 코드
}
