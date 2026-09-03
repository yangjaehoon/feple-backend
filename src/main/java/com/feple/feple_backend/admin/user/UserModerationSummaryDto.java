package com.feple.feple_backend.admin.user;

/**
 * 회원 상세 페이지 상단에 노출하는 모더레이션 요약 — "이 유저가 상습적으로 문제 글을 썼는지"를
 * 한눈에 보기 위한 지표 묶음. 누적 신고 수는 기존 {@code stats.reportCount}로 이미 표시되므로
 * 여기서는 중복하지 않는다.
 *
 * <p>지표는 전부 <b>모더레이션 행위</b> 기준이다 — 본인 삭제처럼 정상 활동으로도 생기는 값은
 * 담지 않는다(오탐 방지). 블라인드는 자동(신고 누적)·수동 모두 관리자 개입으로만 발생한다.
 *
 * @param blindedPostCount 현재 블라인드 상태인 이 유저의 게시글 수(삭제 제외)
 * @param priorBanCount    이 유저를 대상으로 한 정지 조치 누적 횟수(현재 정지 여부와 별개)
 * @param joinedDaysAgo    가입 후 경과 일수(신규 계정 스팸 신호). 가입일 불명이면 -1
 */
public record UserModerationSummaryDto(
        long blindedPostCount,
        long priorBanCount,
        long joinedDaysAgo
) {}
