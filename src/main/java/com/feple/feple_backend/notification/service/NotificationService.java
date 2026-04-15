package com.feple.feple_backend.notification.service;

import com.feple.feple_backend.artistfollow.entity.ArtistFollow;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.notification.dto.NotificationDto;
import com.feple.feple_backend.notification.entity.Notification;
import com.feple.feple_backend.notification.entity.NotificationType;
import com.feple.feple_backend.notification.repository.NotificationRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserDeviceTokenRepository;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ArtistFollowRepository artistFollowRepository;
    private final UserDeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;
    private final FcmPushService fcmPushService;

    /**
     * 아티스트가 페스티벌에 추가될 때 팔로워들에게 알림 발송
     * 비동기 처리 — 메인 트랜잭션 지연 없음
     */
    @Async
    @Transactional
    public void notifyNewFestivalForArtist(Long artistId, String artistName,
                                            Long festivalId, String festivalTitle) {
        List<ArtistFollow> follows = artistFollowRepository.findByArtistId(artistId);
        if (follows.isEmpty()) return;

        String title = artistName + "의 새 페스티벌";
        String body = "'" + festivalTitle + "' 일정이 등록됐어요!";

        List<Long> userIds = follows.stream()
                .map(f -> f.getUser().getId())
                .toList();

        // 인앱 알림 저장
        List<User> users = userRepository.findAllById(userIds);
        List<Notification> notifications = users.stream()
                .map(u -> Notification.of(u, NotificationType.NEW_FESTIVAL, title, body, festivalId))
                .toList();
        notificationRepository.saveAll(notifications);
        log.info("[Notification] 인앱 알림 {}건 저장 (artistId={}, festivalId={})",
                notifications.size(), artistId, festivalId);

        // FCM 푸시 발송
        List<String> tokens = deviceTokenRepository.findTokensByUserIds(userIds);
        fcmPushService.sendMulticast(tokens, title, body, String.valueOf(festivalId));
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getMyNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markRead(Long notificationId, Long userId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));
        if (!n.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 알림만 읽음 처리할 수 있습니다.");
        }
        n.markRead();
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadByUserId(userId);
    }
}
