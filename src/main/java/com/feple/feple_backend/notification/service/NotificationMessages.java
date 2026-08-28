package com.feple.feple_backend.notification.service;

import java.util.Map;

/** 알림 메시지 문자열 상수 모음 */
public final class NotificationMessages {

    private NotificationMessages() {}

    // 반려 사유 한→영 매핑은 관리자 템플릿과 동기화가 강제되는 NotificationRejectReasons로 분리했다.

    // 사유 문자열을 영문 맵에서 찾아 없으면 원문 그대로, base에 separator로 이어붙인다.
    // 사유가 없으면 base를 그대로 반환한다.
    private static String appendReason(String base, String separator, Map<String, String> reasonMap, String reason) {
        if (reason == null || reason.isBlank()) return base;
        return base + separator + reasonMap.getOrDefault(reason.trim(), reason);
    }

    public static String newFestivalTitle(String artistName) {
        return artistName + "의 새 페스티벌";
    }

    public static String newFestivalBody(String festivalTitle) {
        return "'" + festivalTitle + "' 일정이 등록됐어요!";
    }

    public static String newFestivalTitleEn(String artistNameEn) {
        String name = (artistNameEn != null && !artistNameEn.isBlank()) ? artistNameEn : "An artist";
        return name + "'s New Festival";
    }

    public static String newFestivalBodyEn(String festivalTitleEn) {
        String title = (festivalTitleEn != null && !festivalTitleEn.isBlank()) ? festivalTitleEn : "A new festival";
        return "'" + title + "' has been added!";
    }

    public static final String CERT_APPROVED_TITLE = "인증이 승인됐어요!";

    public static String certApprovedBody(String festivalTitle) {
        return "'" + festivalTitle + "' 페스티벌 인증이 승인됐습니다.";
    }

    public static final String CERT_APPROVED_TITLE_EN = "Certification Approved!";

    public static String certApprovedBodyEn(String festivalTitleEn) {
        String title = (festivalTitleEn != null && !festivalTitleEn.isBlank()) ? festivalTitleEn : "";
        return title.isEmpty()
                ? "Your festival certification has been approved."
                : "Your certification for '" + title + "' has been approved.";
    }

    public static final String CERT_REJECTED_TITLE = "인증이 거절됐어요.";

    public static String certRejectedBody(String festivalTitle, String reason) {
        String base = "'" + festivalTitle + "' 인증이 거절됐습니다.";
        return (reason != null && !reason.isBlank()) ? base + " 사유: " + reason : base;
    }

    public static final String CERT_REJECTED_TITLE_EN = "Certification Rejected";

    public static String certRejectedBodyEn(String festivalTitleEn, String reason) {
        String title = (festivalTitleEn != null && !festivalTitleEn.isBlank()) ? festivalTitleEn : "";
        String base = title.isEmpty()
                ? "Your festival certification was rejected."
                : "Your certification for '" + title + "' was rejected.";
        return appendReason(base, " Reason: ", NotificationRejectReasons.CERT, reason);
    }

    public static String newCommentTitle(String commenterNickname) {
        return commenterNickname + "님이 댓글을 남겼어요.";
    }

    public static String newCommentBody(String postTitle) {
        return (postTitle != null && !postTitle.isBlank())
                ? "'" + postTitle + "' 게시글에 새 댓글이 달렸습니다."
                : "게시글에 새 댓글이 달렸습니다.";
    }

    public static String newCommentTitleEn(String commenterNickname) {
        return commenterNickname + " left a comment.";
    }

    public static String newCommentBodyEn(String postTitle) {
        return (postTitle != null && !postTitle.isBlank())
                ? "A new comment was posted on '" + postTitle + "'."
                : "A new comment was posted on your post.";
    }

    public static String festivalReminderTitle(int dDay) {
        return dDay == 1 ? "내일 페스티벌이에요!" : "D-" + dDay + " 페스티벌 리마인더";
    }

    public static String festivalReminderBody(String festivalTitle, int dDay) {
        return "'" + festivalTitle + "' 페스티벌이 " + dDay + "일 후 시작해요!";
    }

    public static String festivalReminderTitleEn(int dDay) {
        return dDay == 1 ? "Festival is tomorrow!" : "D-" + dDay + " Festival Reminder";
    }

    public static String festivalReminderBodyEn(String festivalTitleEn, int dDay) {
        String title = (festivalTitleEn != null && !festivalTitleEn.isBlank()) ? festivalTitleEn : "The festival";
        return "'" + title + "' starts in " + dDay + (dDay == 1 ? " day!" : " days!");
    }

    public static final String SONG_REQUEST_APPROVED_TITLE = "노래 요청이 등록됐어요!";

    public static String songRequestApprovedBody(String songTitle, String artistName) {
        return "'" + songTitle + "' 곡이 " + artistName + " 페이지에 등록됐습니다.";
    }

    public static final String SONG_REQUEST_APPROVED_TITLE_EN = "Song request approved!";

    public static String songRequestApprovedBodyEn(String songTitle, String artistName) {
        return "'" + songTitle + "' has been added to " + artistName + "'s page.";
    }

    public static final String SONG_REQUEST_REJECTED_TITLE = "노래 요청이 거절됐어요.";

    public static String songRequestRejectedBody(String songTitle, String reason) {
        String base = "'" + songTitle + "' 요청이 거절됐습니다.";
        return (reason != null && !reason.isBlank()) ? base + " 사유: " + reason : base;
    }

    public static final String SONG_REQUEST_REJECTED_TITLE_EN = "Song request rejected.";

    public static String songRequestRejectedBodyEn(String songTitle, String reason) {
        String base = "Your request for '" + songTitle + "' was rejected.";
        return appendReason(base, " Reason: ", NotificationRejectReasons.SONG, reason);
    }

    public static final String ARTIST_SUGGESTION_PROCESSED_TITLE = "아티스트 신청 결과";

    public static String artistSuggestionProcessedBody(String artistName, String note) {
        String base = "'" + artistName + "' 신청";
        return (note != null && !note.isBlank()) ? base + ": " + note : base + "을 검토했어요.";
    }

    public static final String ARTIST_SUGGESTION_PROCESSED_TITLE_EN = "Artist request reviewed";

    public static String artistSuggestionProcessedBodyEn(String artistName, String note) {
        String base = "Your request for '" + artistName + "'";
        return (note != null && !note.isBlank())
                ? appendReason(base, ": ", NotificationRejectReasons.ARTIST, note)
                : base + " has been reviewed.";
    }

    public static final String FESTIVAL_SUGGESTION_PROCESSED_TITLE = "페스티벌 신청 결과";

    public static String festivalSuggestionProcessedBody(String festivalName, String note) {
        String base = "'" + festivalName + "' 신청";
        return (note != null && !note.isBlank()) ? base + ": " + note : base + "을 검토했어요.";
    }

    public static final String FESTIVAL_SUGGESTION_PROCESSED_TITLE_EN = "Festival request reviewed";

    public static String festivalSuggestionProcessedBodyEn(String festivalName, String note) {
        String base = "Your request for '" + festivalName + "'";
        return (note != null && !note.isBlank())
                ? appendReason(base, ": ", NotificationRejectReasons.FESTIVAL, note)
                : base + " has been reviewed.";
    }

    public static String newReplyTitle(String replierNickname) {
        return replierNickname + "님이 댓글에 답글을 달았어요.";
    }

    public static String newReplyBody(String postTitle) {
        return (postTitle != null && !postTitle.isBlank())
                ? "'" + postTitle + "' 게시글의 내 댓글에 답글이 달렸습니다."
                : "내 댓글에 답글이 달렸습니다.";
    }

    public static String newReplyTitleEn(String replierNickname) {
        return replierNickname + " replied to your comment.";
    }

    public static String newReplyBodyEn(String postTitle) {
        return (postTitle != null && !postTitle.isBlank())
                ? "Someone replied to your comment on '" + postTitle + "'."
                : "Someone replied to your comment.";
    }

    public static String postLikedTitle(String likerNickname) {
        return likerNickname + "님이 게시글을 좋아해요.";
    }

    public static String postLikedBody(String postTitle) {
        return (postTitle != null && !postTitle.isBlank())
                ? "'" + postTitle + "' 게시글에 좋아요가 달렸습니다."
                : "게시글에 좋아요가 달렸습니다.";
    }

    public static String postLikedTitleEn(String likerNickname) {
        return likerNickname + " liked your post.";
    }

    public static String postLikedBodyEn(String postTitle) {
        return (postTitle != null && !postTitle.isBlank())
                ? "Someone liked your post '" + postTitle + "'."
                : "Someone liked your post.";
    }

    public static final String POST_DELETED_BY_ADMIN_TITLE = "게시글이 삭제됐어요.";

    public static String postDeletedByAdminBody(String postTitle) {
        return (postTitle != null && !postTitle.isBlank())
                ? "'" + postTitle + "' 게시글이 운영 정책에 따라 삭제됐습니다."
                : "게시글이 운영 정책에 따라 삭제됐습니다.";
    }

    public static final String POST_DELETED_BY_ADMIN_TITLE_EN = "Your post has been removed.";

    public static String postDeletedByAdminBodyEn(String postTitle) {
        return (postTitle != null && !postTitle.isBlank())
                ? "Your post '" + postTitle + "' was removed for violating our community guidelines."
                : "Your post was removed for violating our community guidelines.";
    }

    public static String adminPointGrantedTitle(int amount) {
        return (amount >= 0 ? "+" : "") + amount + "P가 지급됐어요!";
    }

    public static String adminPointGrantedBody(String reason) {
        return reason;
    }

    public static String adminPointGrantedTitleEn(int amount) {
        return (amount >= 0 ? "+" : "") + amount + "P has been granted!";
    }

    public static String adminPointGrantedBodyEn(int amount) {
        return amount >= 0
                ? amount + "P has been added to your account by an admin."
                : Math.abs(amount) + "P has been deducted from your account by an admin.";
    }
}
