package com.feple.feple_backend.notification.service;

import com.feple.feple_backend.notification.entity.NotificationType;
import com.feple.feple_backend.user.service.DeviceTokenService;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
        sendMulticastInternal(tokens, title, body, null, NotificationType.ADMIN_BROADCAST.name(), null);
    }

    @Override
    @Async
    public void sendMulticast(List<String> tokens, PushMessage message) {
        sendMulticastInternal(tokens, message.title(), message.body(), message.resourceId(),
                message.type().name(), message.imageUrl());
    }

    private void sendMulticastInternal(List<String> tokens, String title, String body,
                               String resourceId, String type, String imageUrl) {
        if (tokens.isEmpty()) return;
        if (FirebaseApp.getApps().isEmpty()) {
            // @Async라 호출부로 예외가 전파되지 않아 이 로그가 유일한 신호 — warn이 아닌
            // error로 남겨야 알림 채널(로그 모니터링)에서 놓치지 않음
            log.error("[FCM] Firebase 미초기화 상태 — 푸시 생략 (app.firebase.credentials 미설정 가능성)");
            return;
        }

        FirebaseMessaging messaging = FirebaseMessaging.getInstance();

        // 500개씩 나눠서 발송
        for (int batchStart = 0; batchStart < tokens.size(); batchStart += BATCH_SIZE) {
            List<String> batch = tokens.subList(batchStart, Math.min(batchStart + BATCH_SIZE, tokens.size()));
            try {
                MulticastMessage message = buildMulticastMessage(batch, title, body, resourceId, type, imageUrl);
                BatchResponse response = messaging.sendEachForMulticast(message);
                log.info("[FCM] 발송 완료 — 성공: {}, 실패: {}",
                        response.getSuccessCount(), response.getFailureCount());

                List<String> staleTokens = extractStaleTokens(response, batch);
                if (!staleTokens.isEmpty()) {
                    deviceTokenService.deleteStaleTokens(staleTokens);
                    log.info("[FCM] 만료 토큰 {}개 삭제", staleTokens.size());
                }
            } catch (FirebaseMessagingException e) {
                log.error("[FCM] 발송 오류", e);
            }
        }
    }

    private MulticastMessage buildMulticastMessage(List<String> batch, String title, String body,
                                                     String resourceId, String type, String imageUrl) {
        Notification.Builder notificationBuilder = Notification.builder()
                .setTitle(title)
                .setBody(body);
        if (imageUrl != null) notificationBuilder.setImage(imageUrl);
        String linkId = resourceId != null ? resourceId : "";
        return MulticastMessage.builder()
                .addAllTokens(batch)
                .setNotification(notificationBuilder.build())
                .putData("type", type)
                // linkId: 알림 타입에 따라 festivalId/postId/artistId 등 어떤 리소스든 담기는
                // 범용 참조 ID. festivalId 키는 이미 배포된 구버전 클라이언트 호환을 위해 당분간
                // 함께 보낸다 — 신버전은 linkId를 우선 사용한다.
                // TODO: linkId를 읽는 구버전 미만 클라이언트 비율이 무시할 수준으로 낮아지면
                // (앱스토어/플레이스토어 버전 통계 확인 후) festivalId 중복 발행 제거할 것.
                .putData("linkId", linkId)
                .putData("festivalId", linkId)
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
                // FCM이 MessagingErrorCode로 분류하지 못하는 실패(네트워크 오류 등)는 code가 null로
                // 남아 원인을 알 수 없었다 — 그 경우 예외 클래스/메시지를 대신 남겨 다음 발생 시 진단 가능하게 한다.
                if (code != null) {
                    log.debug("[FCM] 실패 토큰 ({}): {}", code, batch.get(idx));
                } else {
                    log.debug("[FCM] 실패 토큰 (미분류, {}): {} - {}",
                            ex != null ? ex.getClass().getSimpleName() : "예외 없음",
                            batch.get(idx),
                            ex != null ? ex.getMessage() : "n/a");
                }
                if (code == MessagingErrorCode.UNREGISTERED
                        || code == MessagingErrorCode.INVALID_ARGUMENT) {
                    staleTokens.add(batch.get(idx));
                }
            }
        }
        return staleTokens;
    }
}
