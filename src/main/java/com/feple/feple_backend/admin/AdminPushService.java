package com.feple.feple_backend.admin;

import com.feple.feple_backend.notification.entity.BroadcastNotification;
import com.feple.feple_backend.notification.repository.BroadcastNotificationRepository;
import com.feple.feple_backend.notification.service.FcmPushService;
import com.feple.feple_backend.user.repository.UserDeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPushService {

    private final UserDeviceTokenRepository deviceTokenRepository;
    private final FcmPushService fcmPushService;
    private final BroadcastNotificationRepository broadcastNotificationRepository;

    @Transactional(readOnly = true)
    public long getRegisteredDeviceCount() {
        return deviceTokenRepository.countDistinctUsers();
    }

    @Transactional
    public void sendToAll(String title, String body) {
        broadcastNotificationRepository.save(BroadcastNotification.of(title, body));

        List<String> tokens = deviceTokenRepository.findAllTokens();
        if (tokens.isEmpty()) {
            log.info("[AdminPush] 등록된 디바이스 토큰 없음 — FCM 발송 생략");
            return;
        }
        log.info("[AdminPush] 전체 푸시 발송 시작 — 토큰 {}개, 제목: {}", tokens.size(), title);
        fcmPushService.sendBroadcast(tokens, title, body);
    }
}
