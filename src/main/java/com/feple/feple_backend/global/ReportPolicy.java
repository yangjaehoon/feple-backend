package com.feple.feple_backend.global;

/** 게시글·댓글 신고의 자동 모더레이션 정책 상수. */
public final class ReportPolicy {

    private ReportPolicy() {}

    /**
     * 대기 상태 신고가 이 건수 이상 쌓이면 관리자 검토 전이라도 대상 콘텐츠를 자동으로 블라인드 처리한다.
     * (기각 등으로 대기 건수가 이 값 미만으로 떨어지면 자동 블라인드 해제)
     */
    public static final int AUTO_BLIND_PENDING_THRESHOLD = 5;
}
