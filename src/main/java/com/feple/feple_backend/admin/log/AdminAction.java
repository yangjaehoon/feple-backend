package com.feple.feple_backend.admin.log;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum AdminAction {
    FESTIVAL_CREATE("페스티벌 등록"),
    FESTIVAL_UPDATE("페스티벌 수정"),
    FESTIVAL_DELETE("페스티벌 삭제"),

    ARTIST_CREATE("아티스트 등록"),
    ARTIST_UPDATE("아티스트 수정"),
    ARTIST_DELETE("아티스트 삭제"),
    ARTIST_SUGGESTION_DISMISS("아티스트 신청 처리"),

    USER_BAN("회원 정지"),
    USER_UNBAN("정지 해제"),
    USER_DELETE("회원 탈퇴"),
    USER_BULK_DELETE("회원 일괄 삭제"),
    USER_ROLE_CHANGE("역할 변경"),

    POST_DELETE("게시글 삭제"),
    POST_BULK_DELETE("게시글 일괄 삭제"),
    POST_RESTORE("게시글 복구"),

    COMMENT_DELETE("댓글 삭제"),

    REPORT_DISMISS("신고 기각"),
    REPORT_DELETE("신고 콘텐츠 삭제"),
    REPORT_BULK_DISMISS("일괄 신고 기각"),
    REPORT_BULK_DELETE("일괄 콘텐츠 삭제"),

    BAD_WORD_ADD("금칙어 추가"),
    BAD_WORD_DELETE("금칙어 삭제"),

    NICKNAME_RESTRICTION_ADD("닉네임 제한 추가"),
    NICKNAME_RESTRICTION_DELETE("닉네임 제한 삭제"),

    CERTIFICATION_APPROVE("인증 승인"),
    CERTIFICATION_REJECT("인증 거절"),

    SONG_REQUEST_APPROVE("노래 요청 승인"),
    SONG_REQUEST_REJECT("노래 요청 거절"),

    PUSH_BROADCAST("전체 푸시 발송"),
    PUSH_ARTIST_FOLLOWERS("아티스트 팔로워 발송"),
    PUSH_FESTIVAL_CERTIFIED("페스티벌 인증자 발송"),
    PUSH_TEST("테스트 발송"),

    EXPORT_USERS("회원 내보내기"),
    EXPORT_REPORTS("신고 내보내기"),

    ADMIN_ACCOUNT_CREATE("관리자 계정 생성"),
    ADMIN_ACCOUNT_UPDATE("관리자 계정 수정"),
    ADMIN_ACCOUNT_DELETE("관리자 계정 삭제"),
    ADMIN_ACCOUNT_TOGGLE("계정 활성화 상태 변경");

    private final String label;

    AdminAction(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Map<String, String> actionLabelMap() {
        return Arrays.stream(values())
                .collect(Collectors.toMap(Enum::name, AdminAction::getLabel));
    }
}
