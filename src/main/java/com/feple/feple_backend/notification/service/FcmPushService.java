package com.feple.feple_backend.notification.service;

import com.feple.feple_backend.notification.entity.NotificationType;
import com.feple.feple_backend.user.service.DeviceTokenService;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmPushService implements PushNotificationClient {

    private final DeviceTokenService deviceTokenService;

    private static final int BATCH_SIZE = 500; // FCM multicast 최대 500개

    // 트랜잭션 커밋 직후(afterCommit) 콜백에서 호출되는 경우가 있어 @Async로 새 스레드에서 실행한다.
    // afterCommit 콜백은 원본 트랜잭션의 리소스가 아직 완전히 언바인드되지 않은 상태라,
    // 그 스레드에서 곧바로 deleteStaleTokens()의 새 @Transactional을 열면 Hibernate가
    // 남아있는 트랜잭션 동기화 상태를 오인해 "Executing an update/delete query" 오류를 낸다.
    @Override
    @Async
    public void sendBroadcast(List<String> tokens, String title, String body) {
        sendMulticastInternal(tokens, title, body, null, NotificationType.ADMIN_BROADCAST.name());
    }

    @Override
    @Async
    public void sendMulticast(List<String> tokens, PushMessage message) {
        sendMulticastInternal(tokens, message.title(), message.body(), message.resourceId(), message.type().name());
    }

    private void sendMulticastInternal(List<String> tokens, String title, String body,
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
                MulticastMessage message = buildMulticastMessage(batch, title, body, resourceId, type);
                BatchResponse response = messaging.sendEachForMulticast(message);
                log.info("[FCM] 발송 완료 — 성공: {}, 실패: {}",
                        response.getSuccessCount(), response.getFailureCount());

                List<String> staleTokens = extractStaleTokens(response, batch);
                if (!staleTokens.isEmpty()) {
                    deviceTokenService.deleteStaleTokens(staleTokens);
                    log.info("[FCM] 만료 토큰 {}개 삭제", staleTokens.size());
                }
            } catch (FirebaseMessagingException e) {
                log.error("[FCM] 발송 오류: {}", e.getMessage());
            }
        }
    }

    private MulticastMessage buildMulticastMessage(List<String> batch, String title, String body,
                                                     String resourceId, String type) {
        return MulticastMessage.builder()
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
    }

    private List<String> extractStaleTokens(BatchResponse response, List<String> batch) {
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
        return staleTokens;
    }
}
