package com.feple.feple_backend.notification.service;

import com.feple.feple_backend.notification.entity.NotificationType;
import com.feple.feple_backend.user.service.DeviceTokenService;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmPushService implements PushNotificationClient {

    private final DeviceTokenService deviceTokenService;

    private static final int BATCH_SIZE = 500; // FCM multicast 최대 500개

    @Override
    public void sendBroadcast(List<String> tokens, String title, String body) {
        sendMulticast(tokens, title, body, null, NotificationType.ADMIN_BROADCAST.name());
    }

    @Override
    public void sendMulticast(List<String> tokens, String title, String body,
                               String resourceId, NotificationType type) {
        sendMulticast(tokens, title, body, resourceId, type.name());
    }

    private void sendMulticast(List<String> tokens, String title, String body,
                               String resourceId, String type) {
        if (tokens.isEmpty()) return;
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("[FCM] Firebase 미초기화 상태 — 푸시 생략");
            return;
        }

        FirebaseMessaging messaging = FirebaseMessaging.getInstance();

        // 500개씩 나눠서 발송
        for (int batchStart = 0; batchStart < tokens.size(); batchStart += BATCH_SIZE) {
            List<String> batch = tokens.subList(batchStart, Math.min(batchStart + BATCH_SIZE, tokens.size()));
            try {
                MulticastMessage message = MulticastMessage.builder()
                        .addAllTokens(batch)
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .putData("type", type)
                        .putData("festivalId", resourceId != null ? resourceId : "")
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

                List<SendResponse> responses = response.getResponses();
                List<String> staleTokens = new ArrayList<>();
                for (int idx = 0; idx < responses.size(); idx++) {
                    if (!responses.get(idx).isSuccessful()) {
                        FirebaseMessagingException ex = responses.get(idx).getException();
                        MessagingErrorCode code = ex != null ? ex.getMessagingErrorCode() : null;
                        log.debug("[FCM] 실패 토큰 ({}): {}", code, batch.get(idx));
                        if (code == MessagingErrorCode.UNREGISTERED
                                || code == MessagingErrorCode.INVALID_ARGUMENT) {
                            staleTokens.add(batch.get(idx));
                        }
                    }
                }
                if (!staleTokens.isEmpty()) {
                    deviceTokenService.deleteStaleTokens(staleTokens);
                    log.info("[FCM] 만료 토큰 {}개 삭제", staleTokens.size());
                }
            } catch (FirebaseMessagingException e) {
                log.error("[FCM] 발송 오류: {}", e.getMessage());
            }
        }
    }
}
