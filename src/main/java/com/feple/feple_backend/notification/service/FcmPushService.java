package com.feple.feple_backend.notification.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class FcmPushService {

    private static final int BATCH_SIZE = 500; // FCM multicast 최대 500개

    /**
     * 여러 FCM 토큰에 푸시 발송
     */
    public void sendMulticast(List<String> tokens, String title, String body, String festivalId) {
        if (tokens.isEmpty()) return;
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("[FCM] Firebase 미초기화 상태 — 푸시 생략");
            return;
        }

        FirebaseMessaging messaging = FirebaseMessaging.getInstance();

        // 500개씩 나눠서 발송
        for (int i = 0; i < tokens.size(); i += BATCH_SIZE) {
            List<String> batch = tokens.subList(i, Math.min(i + BATCH_SIZE, tokens.size()));
            try {
                MulticastMessage message = MulticastMessage.builder()
                        .addAllTokens(batch)
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .putData("type", "NEW_FESTIVAL")
                        .putData("festivalId", festivalId != null ? festivalId : "")
                        .setAndroidConfig(AndroidConfig.builder()
                                .setPriority(AndroidConfig.Priority.HIGH)
                                .build())
                        .setApnsConfig(ApnsConfig.builder()
                                .setAps(Aps.builder().setSound("default").build())
                                .build())
                        .build();

                BatchResponse response = messaging.sendEachForMulticast(message);
                log.info("[FCM] 발송 완료 — 성공: {}, 실패: {}",
                        response.getSuccessCount(), response.getFailureCount());

                // 실패 토큰 로깅 (필요 시 DB에서 삭제 가능)
                List<SendResponse> responses = response.getResponses();
                for (int j = 0; j < responses.size(); j++) {
                    if (!responses.get(j).isSuccessful()) {
                        log.debug("[FCM] 실패 토큰: {}", batch.get(j));
                    }
                }
            } catch (FirebaseMessagingException e) {
                log.error("[FCM] 발송 오류: {}", e.getMessage());
            }
        }
    }
}
