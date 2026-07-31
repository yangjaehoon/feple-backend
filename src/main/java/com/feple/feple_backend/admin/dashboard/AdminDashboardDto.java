package com.feple.feple_backend.admin.dashboard;

public record AdminDashboardDto(
        AdminStatsSummary stats,
        AdminPendingItemsSummary pending,
        AdminContentSummary content,
        // 섹션 중 하나라도 조회에 실패해 0/빈 값으로 대체됐는지 — 실제로 데이터가 없는 것과
        // 조회 실패를 화면에서 구분하기 위함 (관리자가 "0건"을 진짜 0건으로 오인하는 것 방지).
        boolean hasLoadError
) {}
