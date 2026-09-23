package com.feple.feple_backend.admin.account;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AdminPermission {

    STATS("통계"),
    FESTIVALS("페스티벌"),
    ARTISTS("아티스트"),
    POSTS("게시글"),
    NOTICES("공지사항"),
    USERS("회원 관리"),
    CERTIFICATIONS("인증 관리"),
    REPORTS("신고 관리"),
    SONG_REQUESTS("노래 요청"),
    BAD_WORDS("금칙어"),
    /**
     * 크롤링·OCR 도구. 이 권한만으로 스크래핑 결과 적용(페스티벌 생성)·라인업 연결·타임테이블 등록까지
     * 가능하다 — 도구 단위로 묶은 의도된 설계이며, FESTIVALS/ARTISTS 권한을 별도로 요구하지 않는다.
     * 대상 도메인 권한까지 요구하려면 apply 계열 엔드포인트에 검사를 추가해야 한다.
     */
    CRAWL("크롤링"),
    LOGS("감사 로그");

    private final String displayName;

    /** Spring Security 권한 문자열. 예) USERS + READ → "PERM_USERS_READ" */
    public String authority(AdminPermissionLevel level) {
        return "PERM_" + name() + "_" + level.name();
    }

    public String readAuthority() {
        return authority(AdminPermissionLevel.READ);
    }

    public String writeAuthority() {
        return authority(AdminPermissionLevel.WRITE);
    }
}
