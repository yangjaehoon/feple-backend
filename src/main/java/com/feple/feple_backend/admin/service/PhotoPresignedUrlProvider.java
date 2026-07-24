package com.feple.feple_backend.admin.service;

import org.springframework.data.domain.Page;

import java.util.Map;

// 사진 신고(ArtistPhotoReportService)만 구현. ReportQueryService 공통 계약에서 분리해
// 사진 신고가 아닌 구현체가 쓰지 않는 메서드를 갖지 않도록 한다(ISP).
public interface PhotoPresignedUrlProvider {
    Map<Long, String> buildPhotoPresignedUrls(Page<?> reports);
}
