package com.feple.feple_backend.admin.push;

import com.feple.feple_backend.admin.support.AdminConstants;
import com.feple.feple_backend.artist.service.ArtistAdminService;
import com.feple.feple_backend.artistfollow.service.ArtistFollowService;
import com.feple.feple_backend.certification.service.FestivalCertificationAdminService;
import com.feple.feple_backend.festival.dto.FestivalFilterCriteria;
import com.feple.feple_backend.festival.service.FestivalService;
import com.feple.feple_backend.global.exception.InvalidRequestException;
import com.feple.feple_backend.notification.entity.BroadcastNotification;
import com.feple.feple_backend.notification.repository.BroadcastNotificationRepository;
import com.feple.feple_backend.notification.service.NotificationService;
import com.feple.feple_backend.notification.service.PushNotificationClient;
import com.feple.feple_backend.user.entity.UserDeviceToken;
import com.feple.feple_backend.user.repository.UserDeviceTokenRepository;
import com.feple.feple_backend.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPushService {

    private final UserDeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final PushNotificationClient fcmPushService;
    private final BroadcastNotificationRepository broadcastNotificationRepository;
    private final ArtistFollowService artistFollowService;
    private final FestivalCertificationAdminService festivalCertificationAdminService;
    private final ArtistAdminService artistService;
    private final FestivalService festivalService;

    @Transactional(readOnly = true)
    public PushFormData getFormData() {
        return new PushFormData(
                getRegisteredDeviceCount(),
                getBroadcastHistory(),
                artistService.getAllArtistsSortedByName(),
                festivalService.getAllFestivals(FestivalFilterCriteria.forAdmin())
        );
    }

    private long getRegisteredDeviceCount() {
        return deviceTokenRepository.countDistinctUsers();
    }

    private List<BroadcastNotificationView> getBroadcastHistory() {
        return broadcastNotificationRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, AdminConstants.BROADCAST_HISTORY_LIMIT))
                .stream().map(BroadcastNotificationView::from).toList();
    }

    @Transactional
    public void sendTest(Long targetUserId, String title, String body) {
        List<String> tokens = deviceTokenRepository.findByUserId(targetUserId)
                .stream()
                .map(UserDeviceToken::getToken)
                .toList();
        if (tokens.isEmpty()) {
            throw new InvalidRequestException("해당 사용자에게 등록된 디바이스 토큰이 없습니다. (userId=" + targetUserId + ")");
        }
        notificationService.saveAdminBroadcastNotification(targetUserId, title, body);
        logAndSend(tokens, title, body, "[AdminPush] 테스트 발송 — userId={}, 토큰 {}개, 제목: {}", targetUserId, tokens.size(), title);
    }

    @Transactional
    public void sendToArtistFollowers(Long artistId, String title, String body) {
        List<Long> userIds = artistFollowService.getFollowerUserIds(artistId);
        List<String> tokens = resolveTargetTokens(userIds, "해당 아티스트의 팔로워가 없습니다.", "팔로워");
        notificationService.saveAdminBroadcastNotifications(userIds, title, body);
        logAndSend(tokens, title, body, "[AdminPush] 아티스트 팔로워 발송 — artistId={}, 팔로워 {}명, 토큰 {}개, 제목: {}",
                artistId, userIds.size(), tokens.size(), title);
    }

    @Transactional
    public void sendToFestivalCertified(Long festivalId, String title, String body) {
        List<Long> userIds = List.copyOf(festivalCertificationAdminService.getApprovedUserIds(festivalId));
        List<String> tokens = resolveTargetTokens(userIds, "해당 페스티벌의 인증된 참여자가 없습니다.", "인증자");
        notificationService.saveAdminBroadcastNotifications(userIds, title, body);
        logAndSend(tokens, title, body, "[AdminPush] 페스티벌 인증자 발송 — festivalId={}, 인증자 {}명, 토큰 {}개, 제목: {}",
                festivalId, userIds.size(), tokens.size(), title);
    }

    /**
     * 대상 userId 목록 → 발송 대상 없음 검증 → 디바이스 토큰 조회 → 토큰 없음 검증까지 수행한다.
     * sendToArtistFollowers/sendToFestivalCertified에 공통된 검증 절차를 하나로 묶은 것.
     */
    private List<String> resolveTargetTokens(List<Long> userIds, String noTargetMessage, String targetLabel) {
        if (userIds.isEmpty()) {
            throw new InvalidRequestException(noTargetMessage);
        }
        List<String> tokens = deviceTokenRepository.findTokensByUserIds(userIds);
        if (tokens.isEmpty()) {
            throw new InvalidRequestException("발송 대상 기기가 없습니다. (" + targetLabel + " " + userIds.size() + "명 모두 알림 비활성)");
        }
        return tokens;
    }

    @Transactional
    public void sendToAll(String title, String body) {
        List<String> tokens = deviceTokenRepository.findAllTokens();
        if (tokens.isEmpty()) {
            throw new InvalidRequestException("등록된 디바이스 토큰이 없습니다.");
        }
        // 특정 대상 발송(sendToArtistFollowers, sendToFestivalCertified)과 동일하게 유저별 Notification을
        // 저장해 개인 알림 목록에서 읽음/삭제가 가능하게 한다. 이렇게 해야 발송 시점에 존재하지 않던(=아직
        // 가입 전인) 유저에게는 애초에 row가 생기지 않아, 나중에 가입한 유저가 예전 공지를 새 알림처럼
        // 보는 문제가 구조적으로 발생하지 않는다.
        // BroadcastNotification은 관리자 발송 이력 조회(getBroadcastHistory) 전용으로만 남기고,
        // 개인 알림 목록에는 더 이상 노출하지 않는다.
        List<Long> userIds = userRepository.findAllActiveIds();
        notificationService.saveAdminBroadcastNotifications(userIds, title, body);
        broadcastNotificationRepository.save(BroadcastNotification.of(title, body));
        logAndSend(tokens, title, body, "[AdminPush] 전체 푸시 발송 시작 — 대상 {}명, 토큰 {}개, 제목: {}",
                userIds.size(), tokens.size(), title);
    }

    /**
     * FCM 발송(네트워크 I/O)을 DB 트랜잭션 커밋 후로 미룬다 — FileStorageService.deleteFileAfterCommit과 동일한 이유:
     * FCM 응답이 지연되면 트랜잭션 안에서 DB 커넥션을 계속 점유하게 되어 커넥션 풀 고갈로 이어질 수 있다.
     */
    private void logAndSend(List<String> tokens, String title, String body, String logMessage, Object... logArgs) {
        log.info(logMessage, logArgs);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fcmPushService.sendBroadcast(tokens, title, body);
                }
            });
        } else {
            fcmPushService.sendBroadcast(tokens, title, body);
        }
    }
}
