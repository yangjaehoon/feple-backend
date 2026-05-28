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

    @Transactional(readOnly = true)
    public List<BroadcastNotification> getBroadcastHistory() {
        return broadcastNotificationRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public void sendTest(Long targetUserId, String title, String body) {
        List<String> tokens = deviceTokenRepository.findByUserId(targetUserId)
                .stream()
                .map(t -> t.getToken())
                .toList();
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("해당 사용자에게 등록된 디바이스 토큰이 없습니다. (userId=" + targetUserId + ")");
        }
        log.info("[AdminPush] 테스트 발송 — userId={}, 토큰 {}개, 제목: {}", targetUserId, tokens.size(), title);
        fcmPushService.sendBroadcast(tokens, title, body);
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
