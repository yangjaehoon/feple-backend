package com.feple.feple_backend.admin;

import com.feple.feple_backend.admin.system.BroadcastNotificationView;
import com.feple.feple_backend.admin.system.PushFormData;
import com.feple.feple_backend.artist.service.ArtistService;
import com.feple.feple_backend.artistfollow.entity.ArtistFollow;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.certification.repository.FestivalCertificationRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.service.FestivalService;
import com.feple.feple_backend.notification.entity.BroadcastNotification;
import com.feple.feple_backend.notification.entity.Notification;
import com.feple.feple_backend.notification.entity.NotificationType;
import com.feple.feple_backend.notification.repository.BroadcastNotificationRepository;
import com.feple.feple_backend.notification.repository.NotificationRepository;
import com.feple.feple_backend.notification.service.PushNotificationClient;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.UserDeviceToken;
import com.feple.feple_backend.user.repository.UserDeviceTokenRepository;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPushService {

    private final UserDeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final PushNotificationClient fcmPushService;
    private final BroadcastNotificationRepository broadcastNotificationRepository;
    private final ArtistFollowRepository artistFollowRepository;
    private final FestivalCertificationRepository festivalCertificationRepository;
    private final ArtistService artistService;
    private final FestivalService festivalService;

    @Transactional(readOnly = true)
    public PushFormData getFormData() {
        return new PushFormData(
                getRegisteredDeviceCount(),
                getBroadcastHistory(),
                artistService.getAllArtistsSortedByName(),
                festivalService.getAllFestivals(null, null, null, true, null)
        );
    }

    private long getRegisteredDeviceCount() {
        return deviceTokenRepository.countDistinctUsers();
    }

    private List<BroadcastNotificationView> getBroadcastHistory() {
        return broadcastNotificationRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 100))
                .stream().map(BroadcastNotificationView::from).toList();
    }

    @Transactional
    public void sendTest(Long targetUserId, String title, String body) {
        List<String> tokens = deviceTokenRepository.findByUserId(targetUserId)
                .stream()
                .map(UserDeviceToken::getToken)
                .toList();
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("해당 사용자에게 등록된 디바이스 토큰이 없습니다. (userId=" + targetUserId + ")");
        }
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. (userId=" + targetUserId + ")"));
        notificationRepository.save(
                Notification.of(user, NotificationType.ADMIN_BROADCAST, title, body, null, null, (Festival) null));
        log.info("[AdminPush] 테스트 발송 — userId={}, 토큰 {}개, 제목: {}", targetUserId, tokens.size(), title);
        fcmPushService.sendBroadcast(tokens, title, body);
    }

    @Transactional
    public void sendToArtistFollowers(Long artistId, String title, String body) {
        List<Long> userIds = artistFollowRepository.findByArtistId(artistId)
                .stream().map(ArtistFollow::getUserId).toList();
        if (userIds.isEmpty()) {
            throw new IllegalArgumentException("해당 아티스트의 팔로워가 없습니다.");
        }
        List<String> tokens = deviceTokenRepository.findTokensByUserIds(userIds);
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("발송 대상 기기가 없습니다. (팔로워 " + userIds.size() + "명 모두 알림 비활성)");
        }
        saveTargetedNotifications(userIds, title, body);
        log.info("[AdminPush] 아티스트 팔로워 발송 — artistId={}, 팔로워 {}명, 토큰 {}개, 제목: {}", artistId, userIds.size(), tokens.size(), title);
        fcmPushService.sendBroadcast(tokens, title, body);
    }

    @Transactional
    public void sendToFestivalCertified(Long festivalId, String title, String body) {
        Set<Long> userIds = festivalCertificationRepository.findApprovedUserIdsByFestivalId(festivalId);
        if (userIds.isEmpty()) {
            throw new IllegalArgumentException("해당 페스티벌의 인증된 참여자가 없습니다.");
        }
        List<String> tokens = deviceTokenRepository.findTokensByUserIds(List.copyOf(userIds));
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("발송 대상 기기가 없습니다. (인증자 " + userIds.size() + "명 모두 알림 비활성)");
        }
        saveTargetedNotifications(List.copyOf(userIds), title, body);
        log.info("[AdminPush] 페스티벌 인증자 발송 — festivalId={}, 인증자 {}명, 토큰 {}개, 제목: {}", festivalId, userIds.size(), tokens.size(), title);
        fcmPushService.sendBroadcast(tokens, title, body);
    }

    private void saveTargetedNotifications(List<Long> userIds, String title, String body) {
        List<User> users = userRepository.findAllById(userIds);
        notificationRepository.saveAll(users.stream()
                .map(u -> Notification.of(u, NotificationType.ADMIN_BROADCAST, title, body, null, null, (Festival) null))
                .toList());
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
