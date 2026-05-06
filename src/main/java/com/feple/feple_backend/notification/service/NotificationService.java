package com.feple.feple_backend.notification.service;

import com.feple.feple_backend.artistfollow.entity.ArtistFollow;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.notification.entity.Notification;
import com.feple.feple_backend.notification.entity.NotificationType;
import com.feple.feple_backend.notification.repository.NotificationRepository;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.repository.PostRepository;
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
    private final FestivalRepository festivalRepository;
    private final PostRepository postRepository;
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

        Festival festival = festivalRepository.findById(festivalId).orElse(null);
        if (festival == null) return;

        String title = NotificationMessages.newFestivalTitle(artistName);
        String body = NotificationMessages.newFestivalBody(festivalTitle);

        List<Long> userIds = follows.stream()
                .map(f -> f.getUser().getId())
                .toList();

        // 인앱 알림 저장
        List<User> users = userRepository.findAllById(userIds);
        List<Notification> notifications = users.stream()
                .map(u -> Notification.of(u, NotificationType.NEW_FESTIVAL, title, body, festival))
                .toList();
        notificationRepository.saveAll(notifications);
        log.info("[Notification] 인앱 알림 {}건 저장 (artistId={}, festivalId={})",
                notifications.size(), artistId, festivalId);

        // FCM 푸시 발송
        List<String> tokens = deviceTokenRepository.findTokensByUserIds(userIds);
        fcmPushService.sendMulticast(tokens, title, body, String.valueOf(festivalId));
    }

    /** 인증 승인 알림 */
    @Async
    @Transactional
    public void notifyCertApproved(Long userId, String festivalTitle, Long festivalId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        Festival festival = festivalRepository.findById(festivalId).orElse(null);
        String title = NotificationMessages.CERT_APPROVED_TITLE;
        String body = NotificationMessages.certApprovedBody(festivalTitle);

        notificationRepository.save(
                Notification.of(user, NotificationType.CERT_APPROVED, title, body, festival));

        List<String> tokens = deviceTokenRepository.findTokensByUserIds(List.of(userId));
        fcmPushService.sendMulticast(tokens, title, body, String.valueOf(festivalId));
    }

    /** 인증 거절 알림 */
    @Async
    @Transactional
    public void notifyCertRejected(Long userId, String festivalTitle, Long festivalId, String reason) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        Festival festival = festivalRepository.findById(festivalId).orElse(null);
        String title = NotificationMessages.CERT_REJECTED_TITLE;
        String body = NotificationMessages.certRejectedBody(festivalTitle, reason);

        notificationRepository.save(
                Notification.of(user, NotificationType.CERT_REJECTED, title, body, festival));

        List<String> tokens = deviceTokenRepository.findTokensByUserIds(List.of(userId));
        fcmPushService.sendMulticast(tokens, title, body, String.valueOf(festivalId));
    }

    /** 내 게시글에 댓글 알림 */
    @Async
    @Transactional
    public void notifyNewComment(Long postAuthorId, String commenterNickname,
                                  String postTitle, Long postId) {
        // 자기 자신의 댓글이면 알림 없음
        User author = userRepository.findById(postAuthorId).orElse(null);
        if (author == null) return;

        String title = NotificationMessages.newCommentTitle(commenterNickname);
        String body = NotificationMessages.newCommentBody(postTitle);

        Post post = postRepository.findById(postId).orElse(null);
        notificationRepository.save(
                Notification.of(author, NotificationType.NEW_COMMENT, title, body, post));

        List<String> tokens = deviceTokenRepository.findTokensByUserIds(List.of(postAuthorId));
        fcmPushService.sendMulticast(tokens, title, body, null);
    }

    /** 페스티벌 D-day 리마인더 (스케줄러에서 호출) */
    @Transactional
    public void sendFestivalReminders(Long festivalId, String festivalTitle,
                                       List<Long> userIds, int dDay) {
        if (userIds.isEmpty()) return;

        String title = NotificationMessages.festivalReminderTitle(dDay);
        String body = NotificationMessages.festivalReminderBody(festivalTitle, dDay);

        Festival festival = festivalRepository.findById(festivalId).orElse(null);
        List<User> users = userRepository.findAllById(userIds);
        List<Notification> notifications = users.stream()
                .map(u -> Notification.of(u, NotificationType.FESTIVAL_REMINDER, title, body, festival))
                .toList();
        notificationRepository.saveAll(notifications);

        List<String> tokens = deviceTokenRepository.findTokensByUserIds(userIds);
        fcmPushService.sendMulticast(tokens, title, body, String.valueOf(festivalId));
        log.info("[Notification] D-{} 리마인더 {}건 발송 (festivalId={})", dDay, users.size(), festivalId);
    }

}
