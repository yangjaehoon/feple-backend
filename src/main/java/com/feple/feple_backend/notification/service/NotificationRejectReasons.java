package com.feple.feple_backend.notification.service;

import java.util.Map;

/**
 * 관리자 반려/처리 사유(한국어) → 영문 번역 매핑.
 *
 * <p>키는 관리자 화면(templates/admin/**)의 사유 칩·셀렉트 값과 <b>글자 단위로 정확히</b> 일치해야 한다.
 * 불일치 시 영문 사용자에게 한국어 원문이 그대로 노출된다(NotificationMessages.appendReason의 폴백).
 * 템플릿과의 동기화는 RejectReasonTemplateSyncTest가 CI에서 강제한다.
 */
public final class NotificationRejectReasons {

    private NotificationRejectReasons() {}

    public static final Map<String, String> ARTIST = Map.of(
        "요청하신 아티스트를 등록했어요!", "Your requested artist has been registered!",
        "이미 등록된 아티스트예요.", "The artist is already in our database.",
        "정보가 부족해서 등록이 어려워요.", "We couldn't register the artist due to insufficient information.",
        "활동 이력이 없어서 등록이 어려워요.", "The artist doesn't have enough activity history for registration."
    );

    public static final Map<String, String> FESTIVAL = Map.of(
        "요청하신 페스티벌을 등록했어요!", "Your requested festival has been registered!",
        "이미 등록된 페스티벌이에요.", "The festival is already in our database.",
        "정보가 부족해서 등록이 어려워요.", "We couldn't register the festival due to insufficient information.",
        "아직 개최가 확정되지 않았어요.", "The festival hasn't been confirmed to take place yet."
    );

    public static final Map<String, String> CERT = Map.of(
        "사진이 불분명해요.", "The submitted photo is unclear.",
        "해당 페스티벌 인증이 아닌 것 같아요.", "This doesn't appear to be a valid festival certification.",
        "이미 인증된 내역이 있어요.", "You already have a certification for this festival."
    );

    public static final Map<String, String> SONG = Map.of(
        "이미 등록된 곡이에요.", "This song is already in our database.",
        "해당 아티스트의 곡이 아닌 것 같아요.", "This doesn't appear to be this artist's song.",
        "정보가 부족해요.", "Insufficient information provided."
    );
}
