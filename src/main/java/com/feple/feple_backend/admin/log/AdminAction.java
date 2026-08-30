package com.feple.feple_backend.admin.log;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public enum AdminAction {
    FESTIVAL_CREATE("페스티벌 등록"),
    FESTIVAL_UPDATE("페스티벌 수정"),
    FESTIVAL_DELETE("페스티벌 삭제"),
    FESTIVAL_RESTORE("페스티벌 복구"),
    FESTIVAL_ARTIST_ADD("페스티벌 아티스트 추가"),
    FESTIVAL_ARTIST_REMOVE("페스티벌 아티스트 제거"),
    FESTIVAL_ARTIST_UPDATE("페스티벌 라인업 수정"),
    FESTIVAL_BOOTH_ADD("부스 추가"),
    FESTIVAL_BOOTH_DELETE("부스 삭제"),
    FESTIVAL_TICKET_LINK_ADD("예매 링크 추가"),
    FESTIVAL_TICKET_LINK_DELETE("예매 링크 삭제"),
    FESTIVAL_STAGE_ADD("스테이지 추가"),
    FESTIVAL_STAGE_DELETE("스테이지 삭제"),
    FESTIVAL_TIMETABLE_ADD("타임테이블 추가"),
    FESTIVAL_TIMETABLE_UPDATE("타임테이블 수정"),
    FESTIVAL_TIMETABLE_DELETE("타임테이블 삭제"),
    FESTIVAL_CHECKLIST_TOGGLE("페스티벌 체크리스트 변경"),
    FESTIVAL_CHECKLIST_MEMO("페스티벌 체크리스트 메모 저장"),
    FESTIVAL_SCRAPE_CREATE("스크래핑 페스티벌 등록"),
    FESTIVAL_SUGGESTION_APPROVE("페스티벌 신청 승인"),
    FESTIVAL_SUGGESTION_DISMISS("페스티벌 신청 기각"),
    TIMETABLE_OCR_APPLY("타임테이블 OCR 적용"),
    LINEUP_OCR_APPLY("라인업 OCR 적용"),
    UNMATCHED_SUGGESTION_DELETE("미매칭 제안 삭제"),

    ARTIST_CREATE("아티스트 등록"),
    ARTIST_UPDATE("아티스트 수정"),
    ARTIST_DELETE("아티스트 삭제"),
    ARTIST_RESTORE("아티스트 복구"),
    ARTIST_SUGGESTION_APPROVE("아티스트 신청 승인"),
    ARTIST_SUGGESTION_DISMISS("아티스트 신청 기각"),
    ARTIST_SONG_CREATE("아티스트 곡 등록"),
    ARTIST_SONG_DELETE("아티스트 곡 삭제"),
    ARTIST_SETLIST_SAVE("아티스트 셋리스트 저장"),

    USER_BAN("회원 정지"),
    USER_UNBAN("정지 해제"),
    USER_DELETE("회원 탈퇴"),
    USER_BULK_DELETE("회원 일괄 삭제"),
    USER_HARD_DELETE("회원 DB 완전 삭제"),
    USER_ROLE_CHANGE("역할 변경"),
    USER_POINT_GRANT("포인트 지급"),

    POST_DELETE("게시글 삭제"),
    POST_BULK_DELETE("게시글 일괄 삭제"),
    POST_RESTORE("게시글 복구"),
    POST_PIN_TOGGLE("게시글 고정 토글"),

    COMMENT_DELETE("댓글 삭제"),

    NOTICE_CREATE("공지사항 등록"),
    NOTICE_UPDATE("공지사항 수정"),
    NOTICE_DELETE("공지사항 삭제"),
    NOTICE_PIN_TOGGLE("공지사항 고정 토글"),
    NOTICE_PUSH("공지사항 알림 발송"),

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
    CERTIFICATION_BULK_APPROVE("인증 일괄 승인"),
    CERTIFICATION_BULK_REJECT("인증 일괄 거절"),

    SONG_REQUEST_APPROVE("노래 요청 승인"),
    SONG_REQUEST_REJECT("노래 요청 거절"),

    SETLIST_REQUEST_RESOLVE("셋리스트 수정 요청 처리"),

    PUSH_BROADCAST("전체 푸시 발송"),
    PUSH_ARTIST_FOLLOWERS("아티스트 팔로워 발송"),
    PUSH_FESTIVAL_CERTIFIED("페스티벌 인증자 발송"),
    PUSH_TEST("테스트 발송"),

    EXPORT_USERS("회원 내보내기"),
    EXPORT_REPORTS("신고 내보내기"),
    EXPORT_ARTISTS("아티스트 내보내기"),
    EXPORT_FESTIVALS("페스티벌 내보내기"),

    ADMIN_ACCOUNT_CREATE("관리자 계정 생성"),
    ADMIN_ACCOUNT_UPDATE("관리자 계정 수정"),
    ADMIN_ACCOUNT_DELETE("관리자 계정 삭제"),
    ADMIN_ACCOUNT_TOGGLE("계정 활성화 상태 변경"),

    LOGIN_SUCCESS("로그인 성공"),
    LOGIN_FAILURE("로그인 실패"),
    LOGOUT("로그아웃");

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

    /** 관리자 활동 화면(logs.html, dashboard/home.html)에서 배지 색상을 정하는 데 쓰는 위험도 — 판정 기준을 여기 한 곳에 둔다. */
    public enum Severity { DANGER, SUCCESS, INFO }

    // 이름 부분 문자열 매칭(예전 방식)은 UNBAN이 BAN에 걸리는 식의 오판이 반복돼(죽은 분기 버그 이력),
    // 액션별로 명시 분류한다. 여기에 없는 새 액션은 INFO로 취급되므로 위험/성공 성격이면 아래 집합에 추가할 것.
    private static final Set<AdminAction> DANGER_ACTIONS = EnumSet.of(
            FESTIVAL_DELETE, FESTIVAL_ARTIST_REMOVE, FESTIVAL_BOOTH_DELETE, FESTIVAL_TICKET_LINK_DELETE,
            FESTIVAL_STAGE_DELETE, FESTIVAL_TIMETABLE_DELETE, FESTIVAL_SUGGESTION_DISMISS,
            UNMATCHED_SUGGESTION_DELETE,
            ARTIST_DELETE, ARTIST_SUGGESTION_DISMISS, ARTIST_SONG_DELETE,
            USER_BAN, USER_DELETE, USER_BULK_DELETE, USER_HARD_DELETE,
            POST_DELETE, POST_BULK_DELETE, COMMENT_DELETE,
            NOTICE_DELETE,
            REPORT_DELETE, REPORT_BULK_DELETE,
            BAD_WORD_DELETE, NICKNAME_RESTRICTION_DELETE,
            CERTIFICATION_REJECT, CERTIFICATION_BULK_REJECT,
            SONG_REQUEST_REJECT,
            ADMIN_ACCOUNT_DELETE,
            LOGIN_FAILURE);

    private static final Set<AdminAction> SUCCESS_ACTIONS = EnumSet.of(
            FESTIVAL_CREATE, FESTIVAL_RESTORE, FESTIVAL_ARTIST_ADD, FESTIVAL_BOOTH_ADD, FESTIVAL_TICKET_LINK_ADD,
            FESTIVAL_STAGE_ADD, FESTIVAL_TIMETABLE_ADD, FESTIVAL_SCRAPE_CREATE, FESTIVAL_SUGGESTION_APPROVE,
            ARTIST_CREATE, ARTIST_RESTORE, ARTIST_SUGGESTION_APPROVE, ARTIST_SONG_CREATE,
            USER_UNBAN,
            POST_RESTORE, NOTICE_CREATE,
            BAD_WORD_ADD, NICKNAME_RESTRICTION_ADD,
            CERTIFICATION_APPROVE, CERTIFICATION_BULK_APPROVE,
            SONG_REQUEST_APPROVE,
            ADMIN_ACCOUNT_CREATE);

    public Severity severity() {
        if (DANGER_ACTIONS.contains(this)) {
            return Severity.DANGER;
        }
        if (SUCCESS_ACTIONS.contains(this)) {
            return Severity.SUCCESS;
        }
        return Severity.INFO;
    }
}
