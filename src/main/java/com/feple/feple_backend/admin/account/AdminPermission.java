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
    USERS("회원 관리"),
    CERTIFICATIONS("인증 관리"),
    REPORTS("신고 관리"),
    SONG_REQUESTS("노래 요청"),
    BAD_WORDS("금칙어"),
    CRAWL("크롤링"),
    LOGS("감사 로그");

    private final String displayName;
}
