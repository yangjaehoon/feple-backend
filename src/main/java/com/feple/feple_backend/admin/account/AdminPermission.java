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
