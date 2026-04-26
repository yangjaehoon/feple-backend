package com.feple.feple_backend.notification.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMessagesTest {

    // ── newFestivalTitle ──────────────────────────────────────────
    @Test
    void 새_페스티벌_제목_포맷() {
        assertThat(NotificationMessages.newFestivalTitle("아이유"))
                .isEqualTo("아이유의 새 페스티벌");
    }

    @Test
    void 새_페스티벌_본문_포맷() {
        assertThat(NotificationMessages.newFestivalBody("서울재즈페스티벌"))
                .isEqualTo("'서울재즈페스티벌' 일정이 등록됐어요!");
    }

    // ── certApproved ──────────────────────────────────────────────
    @Test
    void 인증_승인_제목은_고정_문자열() {
        assertThat(NotificationMessages.CERT_APPROVED_TITLE)
                .isEqualTo("인증이 승인됐어요!");
    }

    @Test
    void 인증_승인_본문_포맷() {
        assertThat(NotificationMessages.certApprovedBody("록페스티벌"))
                .isEqualTo("'록페스티벌' 페스티벌 인증이 승인됐습니다.");
    }

    // ── certRejected ──────────────────────────────────────────────
    @Test
    void 인증_거절_제목은_고정_문자열() {
        assertThat(NotificationMessages.CERT_REJECTED_TITLE)
                .isEqualTo("인증이 거절됐어요.");
    }

    @Test
    void 인증_거절_본문_사유_있을때() {
        String body = NotificationMessages.certRejectedBody("록페스티벌", "사진이 불명확합니다");
        assertThat(body)
                .contains("'록페스티벌' 인증이 거절됐습니다.")
                .contains("사유: 사진이 불명확합니다");
    }

    @Test
    void 인증_거절_본문_사유_null일때() {
        String body = NotificationMessages.certRejectedBody("록페스티벌", null);
        assertThat(body)
                .isEqualTo("'록페스티벌' 인증이 거절됐습니다.")
                .doesNotContain("사유:");
    }

    @Test
    void 인증_거절_본문_사유_빈문자열일때() {
        String body = NotificationMessages.certRejectedBody("록페스티벌", "  ");
        assertThat(body).doesNotContain("사유:");
    }

    // ── newComment ────────────────────────────────────────────────
    @Test
    void 댓글_알림_제목_포맷() {
        assertThat(NotificationMessages.newCommentTitle("도비노"))
                .isEqualTo("도비노님이 댓글을 남겼어요.");
    }

    @Test
    void 댓글_알림_본문_게시글_제목_있을때() {
        String body = NotificationMessages.newCommentBody("내 첫 게시글");
        assertThat(body).isEqualTo("'내 첫 게시글' 게시글에 새 댓글이 달렸습니다.");
    }

    @Test
    void 댓글_알림_본문_게시글_제목_null일때() {
        assertThat(NotificationMessages.newCommentBody(null))
                .isEqualTo("게시글에 새 댓글이 달렸습니다.");
    }

    @Test
    void 댓글_알림_본문_게시글_제목_빈문자열일때() {
        assertThat(NotificationMessages.newCommentBody(""))
                .isEqualTo("게시글에 새 댓글이 달렸습니다.");
    }

    // ── festivalReminder ──────────────────────────────────────────
    @Test
    void 리마인더_제목_D1() {
        assertThat(NotificationMessages.festivalReminderTitle(1))
                .isEqualTo("내일 페스티벌이에요!");
    }

    @Test
    void 리마인더_제목_D7() {
        assertThat(NotificationMessages.festivalReminderTitle(7))
                .isEqualTo("D-7 페스티벌 리마인더");
    }

    @Test
    void 리마인더_본문_포맷() {
        assertThat(NotificationMessages.festivalReminderBody("서울재즈페스티벌", 3))
                .isEqualTo("'서울재즈페스티벌' 페스티벌이 3일 후 시작해요!");
    }
}
