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
import com.feple.feple_backend.notification.entity.NotificationPreference;
import com.feple.feple_backend.artist.song.event.SongRequestApprovedEvent;
import com.feple.feple_backend.artist.song.event.SongRequestRejectedEvent;
import com.feple.feple_backend.artist.suggestion.event.ArtistSuggestionProcessedEvent;
import com.feple.feple_backend.comment.event.CommentCreatedEvent;
import com.feple.feple_backend.post.event.PostLikedEvent;
import com.feple.feple_backend.post.event.PostDeletedByAdminEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private final PushNotificationClient fcmPushService;
    private final NotificationPreferenceService preferenceService;

    /**
     * 아티스트가 페스티벌에 추가될 때 팔로워들에게 알림 발송
     * 비동기 처리 — 메인 트랜잭션 지연 없음
     */
    @Async
    @Transactional
    public void notifyNewFestivalForArtist(Long artistId, String artistName, String artistNameEn,
                                            Long festivalId, String festivalTitle, String festivalTitleEn) {
        List<ArtistFollow> follows = artistFollowRepository.findByArtistId(artistId);
        if (follows.isEmpty()) return;

        Optional<Festival> festivalOpt = festivalRepository.findById(festivalId);
        if (festivalOpt.isEmpty()) return;
        Festival festival = festivalOpt.get();

        String title = NotificationMessages.newFestivalTitle(artistName);
        String body = NotificationMessages.newFestivalBody(festivalTitle);
        String titleEn = NotificationMessages.newFestivalTitleEn(artistNameEn);
        String bodyEn = NotificationMessages.newFestivalBodyEn(festivalTitleEn);

        List<Long> userIds = follows.stream().map(ArtistFollow::getUserId).toList();
        List<User> users = userRepository.findAllById(userIds);

        saveAndPush(users, NotificationType.NEW_FESTIVAL, title, body, titleEn, bodyEn, festival, String.valueOf(festivalId));
        log.info("[Notification] 인앱 알림 {}건 저장 (artistId={}, festivalId={})", users.size(), artistId, festivalId);
    }

    /** 인증 승인 알림 */
    @Async
    @Transactional
    public void notifyCertApproved(Long userId, String festivalTitle, String festivalTitleEn, Long festivalId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) return;
        User user = userOpt.get();
        Festival festival = festivalRepository.findById(festivalId).orElse(null);
        String title = NotificationMessages.CERT_APPROVED_TITLE;
        String body = NotificationMessages.certApprovedBody(festivalTitle);
        String titleEn = NotificationMessages.CERT_APPROVED_TITLE_EN;
        String bodyEn = NotificationMessages.certApprovedBodyEn(festivalTitleEn);
        notificationRepository.save(Notification.of(user, NotificationType.CERT_APPROVED, title, body, titleEn, bodyEn, festival));
        NotificationPreference pref = preferenceService.getOrCreate(userId);
        if (pref.isEnabledFor(NotificationType.CERT_APPROVED)) {
            List<String> tokens = deviceTokenRepository.findTokensByUserIds(List.of(userId));
            fcmPushService.sendMulticast(tokens, title, body, String.valueOf(festivalId), NotificationType.CERT_APPROVED);
        }
    }

    /** 인증 거절 알림 */
    @Async
    @Transactional
    public void notifyCertRejected(Long userId, String festivalTitle, String festivalTitleEn, Long festivalId, String reason) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) return;
        User user = userOpt.get();
        Festival festival = festivalRepository.findById(festivalId).orElse(null);
        String title = NotificationMessages.CERT_REJECTED_TITLE;
        String body = NotificationMessages.certRejectedBody(festivalTitle, reason);
        String titleEn = NotificationMessages.CERT_REJECTED_TITLE_EN;
        String bodyEn = NotificationMessages.certRejectedBodyEn(festivalTitleEn, reason);
        notificationRepository.save(Notification.of(user, NotificationType.CERT_REJECTED, title, body, titleEn, bodyEn, festival));
        NotificationPreference pref = preferenceService.getOrCreate(userId);
        if (pref.isEnabledFor(NotificationType.CERT_REJECTED)) {
            List<String> tokens = deviceTokenRepository.findTokensByUserIds(List.of(userId));
            fcmPushService.sendMulticast(tokens, title, body, String.valueOf(festivalId), NotificationType.CERT_REJECTED);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onSongRequestApproved(SongRequestApprovedEvent event) {
        Optional<User> userOpt = userRepository.findById(event.userId());
        if (userOpt.isEmpty()) return;
        User user = userOpt.get();
        String title = NotificationMessages.SONG_REQUEST_APPROVED_TITLE;
        String body = NotificationMessages.songRequestApprovedBody(event.songTitle(), event.artistName());
        String titleEn = NotificationMessages.SONG_REQUEST_APPROVED_TITLE_EN;
        String bodyEn = NotificationMessages.songRequestApprovedBodyEn(event.songTitle(), event.artistName());
        Festival noFestival = null;
        notificationRepository.save(
                Notification.of(user, NotificationType.SONG_REQUEST_APPROVED, title, body, titleEn, bodyEn, noFestival));
        NotificationPreference pref = preferenceService.getOrCreate(event.userId());
        if (pref.isEnabledFor(NotificationType.SONG_REQUEST_APPROVED)) {
            List<String> tokens = deviceTokenRepository.findTokensByUserIds(List.of(event.userId()));
            fcmPushService.sendMulticast(tokens, title, body, null, NotificationType.SONG_REQUEST_APPROVED);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onSongRequestRejected(SongRequestRejectedEvent event) {
        Optional<User> userOpt = userRepository.findById(event.userId());
        if (userOpt.isEmpty()) return;
        User user = userOpt.get();
        String title = NotificationMessages.SONG_REQUEST_REJECTED_TITLE;
        String body = NotificationMessages.songRequestRejectedBody(event.songTitle(), event.reason());
        String titleEn = NotificationMessages.SONG_REQUEST_REJECTED_TITLE_EN;
        String bodyEn = NotificationMessages.songRequestRejectedBodyEn(event.songTitle(), event.reason());
        Festival noFestival = null;
        notificationRepository.save(
                Notification.of(user, NotificationType.SONG_REQUEST_REJECTED, title, body, titleEn, bodyEn, noFestival));
        NotificationPreference pref = preferenceService.getOrCreate(event.userId());
        if (pref.isEnabledFor(NotificationType.SONG_REQUEST_REJECTED)) {
            List<String> tokens = deviceTokenRepository.findTokensByUserIds(List.of(event.userId()));
            fcmPushService.sendMulticast(tokens, title, body, null, NotificationType.SONG_REQUEST_REJECTED);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onArtistSuggestionProcessed(ArtistSuggestionProcessedEvent event) {
        Optional<User> userOpt = userRepository.findById(event.userId());
        if (userOpt.isEmpty()) return;
        User user = userOpt.get();
        String title = NotificationMessages.ARTIST_SUGGESTION_PROCESSED_TITLE;
        String body = NotificationMessages.artistSuggestionProcessedBody(event.artistName(), event.note());
        String titleEn = NotificationMessages.ARTIST_SUGGESTION_PROCESSED_TITLE_EN;
        String bodyEn = NotificationMessages.artistSuggestionProcessedBodyEn(event.artistName(), event.note());
        notificationRepository.save(
                Notification.of(user, NotificationType.ARTIST_SUGGESTION_PROCESSED, title, body, titleEn, bodyEn, (Festival) null));
        NotificationPreference pref = preferenceService.getOrCreate(event.userId());
        if (pref.isEnabledFor(NotificationType.ARTIST_SUGGESTION_PROCESSED)) {
            List<String> tokens = deviceTokenRepository.findTokensByUserIds(List.of(event.userId()));
            fcmPushService.sendMulticast(tokens, title, body, null, NotificationType.ARTIST_SUGGESTION_PROCESSED);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCommentCreated(CommentCreatedEvent event) {
        if (event.postAuthorId() != null) {
            notifyNewComment(event.postAuthorId(), event.commenterNickname(),
                    event.postTitle(), event.postId());
        }
        if (event.parentCommentAuthorId() != null) {
            notifyNewReply(event.parentCommentAuthorId(), event.commenterNickname(),
                    event.postTitle(), event.postId());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPostLiked(PostLikedEvent event) {
        Optional<User> authorOpt = userRepository.findById(event.postAuthorId());
        if (authorOpt.isEmpty()) return;
        User author = authorOpt.get();
        String title = NotificationMessages.postLikedTitle(event.likerNickname());
        String body = NotificationMessages.postLikedBody(event.postTitle());
        String titleEn = NotificationMessages.postLikedTitleEn(event.likerNickname());
        String bodyEn = NotificationMessages.postLikedBodyEn(event.postTitle());
        Post post = postRepository.findById(event.postId()).orElse(null);
        notificationRepository.save(
                Notification.of(author, NotificationType.POST_LIKED, title, body, titleEn, bodyEn, post));
        NotificationPreference pref = preferenceService.getOrCreate(event.postAuthorId());
        if (pref.isEnabledFor(NotificationType.POST_LIKED)) {
            List<String> tokens = deviceTokenRepository.findTokensByUserIds(List.of(event.postAuthorId()));
            fcmPushService.sendMulticast(tokens, title, body, String.valueOf(event.postId()), NotificationType.POST_LIKED);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPostDeletedByAdmin(PostDeletedByAdminEvent event) {
        Optional<User> authorOpt = userRepository.findById(event.postAuthorId());
        if (authorOpt.isEmpty()) return;
        User author = authorOpt.get();
        String title = NotificationMessages.POST_DELETED_BY_ADMIN_TITLE;
        String body = NotificationMessages.postDeletedByAdminBody(event.postTitle());
        String titleEn = NotificationMessages.POST_DELETED_BY_ADMIN_TITLE_EN;
        String bodyEn = NotificationMessages.postDeletedByAdminBodyEn(event.postTitle());
        notificationRepository.save(
                Notification.of(author, NotificationType.POST_DELETED_BY_ADMIN, title, body, titleEn, bodyEn, (Festival) null));
        NotificationPreference pref = preferenceService.getOrCreate(event.postAuthorId());
        if (pref.isEnabledFor(NotificationType.POST_DELETED_BY_ADMIN)) {
            List<String> tokens = deviceTokenRepository.findTokensByUserIds(List.of(event.postAuthorId()));
            fcmPushService.sendMulticast(tokens, title, body, null, NotificationType.POST_DELETED_BY_ADMIN);
        }
    }

    /** 내 게시글에 댓글 알림 */
    @Async
    @Transactional
    public void notifyNewComment(Long postAuthorId, String commenterNickname,
                                  String postTitle, Long postId) {
        // 자기 자신의 댓글이면 알림 없음
        Optional<User> authorOpt = userRepository.findById(postAuthorId);
        if (authorOpt.isEmpty()) return;
        User author = authorOpt.get();

        String title = NotificationMessages.newCommentTitle(commenterNickname);
        String body = NotificationMessages.newCommentBody(postTitle);
        String titleEn = NotificationMessages.newCommentTitleEn(commenterNickname);
        String bodyEn = NotificationMessages.newCommentBodyEn(postTitle);

        Post post = postRepository.findById(postId).orElse(null);
        notificationRepository.save(
                Notification.of(author, NotificationType.NEW_COMMENT, title, body, titleEn, bodyEn, post));

        NotificationPreference pref = preferenceService.getOrCreate(postAuthorId);
        if (pref.isEnabledFor(NotificationType.NEW_COMMENT)) {
            List<String> tokens = deviceTokenRepository.findTokensByUserIds(List.of(postAuthorId));
            fcmPushService.sendMulticast(tokens, title, body, null, NotificationType.NEW_COMMENT);
        }
    }

    /** 내 댓글에 대댓글 알림 */
    @Async
    @Transactional
    public void notifyNewReply(Long parentCommentAuthorId, String replierNickname,
                                String postTitle, Long postId) {
        Optional<User> authorOpt = userRepository.findById(parentCommentAuthorId);
        if (authorOpt.isEmpty()) return;
        User author = authorOpt.get();
        String title = NotificationMessages.newReplyTitle(replierNickname);
        String body = NotificationMessages.newReplyBody(postTitle);
        String titleEn = NotificationMessages.newReplyTitleEn(replierNickname);
        String bodyEn = NotificationMessages.newReplyBodyEn(postTitle);
        Post post = postRepository.findById(postId).orElse(null);
        notificationRepository.save(
                Notification.of(author, NotificationType.NEW_REPLY, title, body, titleEn, bodyEn, post));
        NotificationPreference pref = preferenceService.getOrCreate(parentCommentAuthorId);
        if (pref.isEnabledFor(NotificationType.NEW_REPLY)) {
            List<String> tokens = deviceTokenRepository.findTokensByUserIds(List.of(parentCommentAuthorId));
            fcmPushService.sendMulticast(tokens, title, body, null, NotificationType.NEW_REPLY);
        }
    }

    /** 페스티벌 D-day 리마인더 (스케줄러에서 호출) */
    @Transactional
    public void sendFestivalReminders(Long festivalId, String festivalTitle, String festivalTitleEn,
                                       List<Long> userIds, int dDay) {
        if (userIds.isEmpty()) return;

        String title = NotificationMessages.festivalReminderTitle(dDay);
        String body = NotificationMessages.festivalReminderBody(festivalTitle, dDay);
        String titleEn = NotificationMessages.festivalReminderTitleEn(dDay);
        String bodyEn = NotificationMessages.festivalReminderBodyEn(festivalTitleEn, dDay);

        Festival festival = festivalRepository.findById(festivalId).orElse(null);
        List<User> users = userRepository.findAllById(userIds);
        saveAndPush(users, NotificationType.FESTIVAL_REMINDER, title, body, titleEn, bodyEn, festival, String.valueOf(festivalId));
        log.info("[Notification] D-{} 리마인더 {}건 발송 (festivalId={})", dDay, users.size(), festivalId);
    }

    private void saveAndPush(List<User> users, NotificationType type,
                              String title, String body, String titleEn, String bodyEn,
                              Festival festival, String linkId) {
        notificationRepository.saveAll(users.stream()
                .map(u -> Notification.of(u, type, title, body, titleEn, bodyEn, festival))
                .toList());
        List<Long> allUserIds = users.stream().map(User::getId).toList();
        Map<Long, com.feple.feple_backend.notification.entity.NotificationPreference> prefMap =
                preferenceService.getOrCreateBatch(allUserIds);
        List<Long> enabledUserIds = allUserIds.stream()
                .filter(id -> prefMap.get(id).isEnabledFor(type))
                .toList();
        List<String> tokens = deviceTokenRepository.findTokensByUserIds(enabledUserIds);
        fcmPushService.sendMulticast(tokens, title, body, linkId, type);
    }

}
