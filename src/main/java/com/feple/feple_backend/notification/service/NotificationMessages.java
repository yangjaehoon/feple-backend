package com.feple.feple_backend.notification.service;

/** 알림 메시지 문자열 상수 모음 */
public final class NotificationMessages {

    private NotificationMessages() {}

    public static String newFestivalTitle(String artistName) {
        return artistName + "의 새 페스티벌";
    }

    public static String newFestivalBody(String festivalTitle) {
        return "'" + festivalTitle + "' 일정이 등록됐어요!";
    }

    public static final String CERT_APPROVED_TITLE = "인증이 승인됐어요!";

    public static String certApprovedBody(String festivalTitle) {
        return "'" + festivalTitle + "' 페스티벌 인증이 승인됐습니다.";
    }

    public static final String CERT_REJECTED_TITLE = "인증이 거절됐어요.";

    public static String certRejectedBody(String festivalTitle, String reason) {
        String base = "'" + festivalTitle + "' 인증이 거절됐습니다.";
        return (reason != null && !reason.isBlank()) ? base + " 사유: " + reason : base;
    }

    public static String newCommentTitle(String commenterNickname) {
        return commenterNickname + "님이 댓글을 남겼어요.";
    }

    public static String newCommentBody(String postTitle) {
        return (postTitle != null && !postTitle.isBlank())
                ? "'" + postTitle + "' 게시글에 새 댓글이 달렸습니다."
                : "게시글에 새 댓글이 달렸습니다.";
    }

    public static String festivalReminderTitle(int dDay) {
        return dDay == 1 ? "내일 페스티벌이에요!" : "D-" + dDay + " 페스티벌 리마인더";
    }

    public static String festivalReminderBody(String festivalTitle, int dDay) {
        return "'" + festivalTitle + "' 페스티벌이 " + dDay + "일 후 시작해요!";
    }

    public static final String SONG_REQUEST_APPROVED_TITLE = "노래 요청이 등록됐어요!";

    public static String songRequestApprovedBody(String songTitle, String artistName) {
        return "'" + songTitle + "' 곡이 " + artistName + " 페이지에 등록됐습니다.";
    }

    public static final String SONG_REQUEST_REJECTED_TITLE = "노래 요청이 거절됐어요.";

    public static String songRequestRejectedBody(String songTitle, String reason) {
        String base = "'" + songTitle + "' 요청이 거절됐습니다.";
        return (reason != null && !reason.isBlank()) ? base + " 사유: " + reason : base;
    }

    public static final String ARTIST_SUGGESTION_PROCESSED_TITLE = "아티스트 신청 결과";

    public static String artistSuggestionProcessedBody(String artistName, String note) {
        return (note != null && !note.isBlank()) ? note : "'" + artistName + "' 신청을 검토했어요.";
    }
}
