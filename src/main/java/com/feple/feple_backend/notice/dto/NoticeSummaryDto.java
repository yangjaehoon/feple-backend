package com.feple.feple_backend.notice.dto;

import com.feple.feple_backend.notice.entity.Notice;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/** 목록 화면(관리자/공개 API 공용)에는 content가 필요 없어 별도 요약 DTO로 분리 — 목록 조회가 매 건 최대 1만자를 실어 나르지 않도록 함. */
@Getter
@Builder
public class NoticeSummaryDto {

    private Long id;
    private String title;
    private boolean pinned;
    private LocalDateTime createdAt;

    public static NoticeSummaryDto from(Notice notice) {
        return NoticeSummaryDto.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .pinned(notice.isPinned())
                .createdAt(notice.getCreatedAt())
                .build();
    }
}
