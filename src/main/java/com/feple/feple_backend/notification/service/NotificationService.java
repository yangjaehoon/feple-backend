package com.feple.feple_backend.notification.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artistfollow.entity.ArtistFollow;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.notification.entity.Notification;
import com.feple.feple_backend.notification.entity.NotificationContent;
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
import com.feple.feple_backend.artistfestival.event.ArtistAddedToFestivalEvent;
import com.feple.feple_backend.certification.event.CertificationApprovedEvent;
import com.feple.feple_backend.certification.event.CertificationRejectedEvent;
import com.feple.feple_backend.comment.event.CommentCreatedEvent;
import com.feple.feple_backend.post.event.PostLikedEvent;
import com.feple.feple_backend.post.event.PostDeletedByAdminEvent;
import com.feple.feple_backend.userblock.service.UserBlockService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.feple.feple_backend.user.repository.UserDeviceTokenRepository.TokenLanguageProjection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ArtistFollowRepository artistFollowRepository;
    private final ArtistRepository artistRepository;
    private final UserDeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;
    private final FestivalRepository festivalRepository;
    private final PostRepository postRepository;
    private final PushNotificationClient fcmPushService;
    private final NotificationPreferenceService preferenceService;
    private final UserBlockService userBlockService;

    private record NotificationMessage(NotificationType type, String title, String body,
                                        String titleEn, String bodyEn, String resourceId) {
        NotificationContent toContent() {
            return new NotificationContent(type, title, body, titleEn, bodyEn);
        }
    }

    /** 아티스트가 페스티벌에 추가될 때 팔로워들에게 알림 발송 — 커밋 후에만 발송 */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onArtistAddedToFestival(ArtistAddedToFestivalEvent event) {
        List<ArtistFollow> follows = artistFollowRepository.findByArtistId(event.artistId());
        if (follows.isEmpty()) return;

        Festival festival = festivalRepository.findById(event.festivalId()).orElse(null);
        if (festival == null) return;

        String title = NotificationMessages.newFestivalTitle(event.artistName());
        String body = NotificationMessages.newFestivalBody(event.festivalTitle());
        String titleEn = NotificationMessages.newFestivalTitleEn(event.artistNameEn());
        String bodyEn = NotificationMessages.newFestivalBodyEn(event.festivalTitleEn());

        List<Long> userIds = follows.stream().map(ArtistFollow::getUserId).toList();
        List<User> users = userRepository.findAllById(userIds);

        NotificationMessage message = new NotificationMessage(
                NotificationType.NEW_FESTIVAL, title, body, titleEn, bodyEn, String.valueOf(event.festivalId()));
        saveAndPush(users, message, festival);
        log.info("[Notification] 인앱 알림 {}건 저장 (artistId={}, festivalId={})", users.size(), event.artistId(), event.festivalId());
    }

    /** 인증 승인 알림 — 커밋 후에만 발송 */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCertificationApproved(CertificationApprovedEvent event) {
        User user = userRepository.findById(event.userId()).orElse(null);
        if (user == null) return;
        Festival festival = festivalRepository.findById(event.festivalId()).orElse(null);
        notifySingle(event.userId(), NotificationType.CERT_APPROVED,
                NotificationMessages.CERT_APPROVED_TITLE,
                NotificationMessages.certApprovedBody(event.festivalTitle()),
                NotificationMessages.CERT_APPROVED_TITLE_EN,
                NotificationMessages.certApprovedBodyEn(event.festivalTitleEn()),
                String.valueOf(event.festivalId()),
                content -> Notification.of(user, content, festival));
    }

    /** 인증 거절 알림 — 커밋 후에만 발송 */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCertificationRejected(CertificationRejectedEvent event) {
        User user = userRepository.findById(event.userId()).orElse(null);
        if (user == null) return;
        Festival festival = festivalRepository.findById(event.festivalId()).orElse(null);
        notifySingle(event.userId(), NotificationType.CERT_REJECTED,
                NotificationMessages.CERT_REJECTED_TITLE,
                NotificationMessages.certRejectedBody(event.festivalTitle(), event.reason()),
                NotificationMessages.CERT_REJECTED_TITLE_EN,
                NotificationMessages.certRejectedBodyEn(event.festivalTitleEn(), event.reason()),
                String.valueOf(event.festivalId()),
                content -> Notification.of(user, content, festival));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSongRequestApproved(SongRequestApprovedEvent event) {
        User user = userRepository.findById(event.userId()).orElse(null);
        if (user == null) return;
        Artist artist = artistRepository.findById(event.artistId()).orElse(null);
        notifySingle(event.userId(), NotificationType.SONG_REQUEST_APPROVED,
                NotificationMessages.SONG_REQUEST_APPROVED_TITLE,
                NotificationMessages.songRequestApprovedBody(event.songTitle(), event.artistName()),
                NotificationMessages.SONG_REQUEST_APPROVED_TITLE_EN,
                NotificationMessages.songRequestApprovedBodyEn(event.songTitle(), event.artistNameEn()),
                String.valueOf(event.artistId()),
                content -> Notification.of(user, content, artist));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSongRequestRejected(SongRequestRejectedEvent event) {
        User user = userRepository.findById(event.userId()).orElse(null);
        if (user == null) return;
        Artist artist = artistRepository.findById(event.artistId()).orElse(null);
        notifySingle(event.userId(), NotificationType.SONG_REQUEST_REJECTED,
                NotificationMessages.SONG_REQUEST_REJECTED_TITLE,
                NotificationMessages.songRequestRejectedBody(event.songTitle(), event.reason()),
                NotificationMessages.SONG_REQUEST_REJECTED_TITLE_EN,
                NotificationMessages.songRequestRejectedBodyEn(event.songTitle(), event.reason()),
                String.valueOf(event.artistId()),
                content -> Notification.of(user, content, artist));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onArtistSuggestionProcessed(ArtistSuggestionProcessedEvent event) {
        User user = userRepository.findById(event.userId()).orElse(null);
        if (user == null) return;
        String resourceId = event.artistId() != null ? String.valueOf(event.artistId()) : null;
        Artist artist = event.artistId() != null ? artistRepository.findById(event.artistId()).orElse(null) : null;
        String artistNameEn = (artist != null && artist.getNameEn() != null && !artist.getNameEn().isBlank())
                ? artist.getNameEn() : event.artistName();
        notifySingle(event.userId(), NotificationType.ARTIST_SUGGESTION_PROCESSED,
                NotificationMessages.ARTIST_SUGGESTION_PROCESSED_TITLE,
                NotificationMessages.artistSuggestionProcessedBody(event.artistName(), event.note()),
                NotificationMessages.ARTIST_SUGGESTION_PROCESSED_TITLE_EN,
                NotificationMessages.artistSuggestionProcessedBodyEn(artistNameEn, event.note()),
                resourceId,
                content -> artist != null ? Notification.of(user, content, artist) : Notification.of(user, content, (Festival) null));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentCreated(CommentCreatedEvent event) {
        if (event.postAuthorId() != null && !userBlockService.isBlocked(event.postAuthorId(), event.commenterId())) {
            notifyNewComment(event.postAuthorId(), event);
        }
        if (event.parentCommentAuthorId() != null && !userBlockService.isBlocked(event.parentCommentAuthorId(), event.commenterId())) {
            notifyNewReply(event.parentCommentAuthorId(), event);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostLiked(PostLikedEvent event) {
        if (userBlockService.isBlocked(event.postAuthorId(), event.likerId())) return;
        User author = userRepository.findById(event.postAuthorId()).orElse(null);
        if (author == null) return;
        Post post = postRepository.findById(event.postId()).orElse(null);
        notifySingle(event.postAuthorId(), NotificationType.POST_LIKED,
                NotificationMessages.postLikedTitle(event.likerNickname()),
                NotificationMessages.postLikedBody(event.postTitle()),
                NotificationMessages.postLikedTitleEn(event.likerNickname()),
                NotificationMessages.postLikedBodyEn(event.postTitle()),
                String.valueOf(event.postId()),
                content -> Notification.of(author, content, post));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostDeletedByAdmin(PostDeletedByAdminEvent event) {
        User author = userRepository.findById(event.postAuthorId()).orElse(null);
        if (author == null) return;
        notifySingle(event.postAuthorId(), NotificationType.POST_DELETED_BY_ADMIN,
                NotificationMessages.POST_DELETED_BY_ADMIN_TITLE,
                NotificationMessages.postDeletedByAdminBody(event.postTitle()),
                NotificationMessages.POST_DELETED_BY_ADMIN_TITLE_EN,
                NotificationMessages.postDeletedByAdminBodyEn(event.postTitle()),
                null,
                content -> Notification.of(author, content, (Festival) null));
    }

    /** 내 게시글에 댓글 알림 — onCommentCreated에서만 호출 (자체 호출이라 별도 @Async/@Transactional 불필요) */
    private void notifyNewComment(Long postAuthorId, CommentCreatedEvent event) {
        // 자기 자신의 댓글이면 알림 없음
        User author = userRepository.findById(postAuthorId).orElse(null);
        if (author == null) return;

        String commenterNickname = event.commenterNickname();
        String postTitle = event.postTitle();
        Post post = postRepository.findById(event.postId()).orElse(null);
        notifySingle(postAuthorId, NotificationType.NEW_COMMENT,
                NotificationMessages.newCommentTitle(commenterNickname),
                NotificationMessages.newCommentBody(postTitle),
                NotificationMessages.newCommentTitleEn(commenterNickname),
                NotificationMessages.newCommentBodyEn(postTitle),
                null,
                content -> Notification.of(author, content, post));
    }

    /** 내 댓글에 대댓글 알림 — onCommentCreated에서만 호출 (자체 호출이라 별도 @Async/@Transactional 불필요) */
    private void notifyNewReply(Long parentCommentAuthorId, CommentCreatedEvent event) {
        User author = userRepository.findById(parentCommentAuthorId).orElse(null);
        if (author == null) return;
        String replierNickname = event.commenterNickname();
        String postTitle = event.postTitle();
        Post post = postRepository.findById(event.postId()).orElse(null);
        notifySingle(parentCommentAuthorId, NotificationType.NEW_REPLY,
                NotificationMessages.newReplyTitle(replierNickname),
                NotificationMessages.newReplyBody(postTitle),
                NotificationMessages.newReplyTitleEn(replierNickname),
                NotificationMessages.newReplyBodyEn(postTitle),
                null,
                content -> Notification.of(author, content, post));
    }

    private void notifySingle(Long userId, NotificationType type,
                               String title, String body, String titleEn, String bodyEn, String resourceId,
                               Function<NotificationContent, Notification> notificationFactory) {
        NotificationMessage message = new NotificationMessage(type, title, body, titleEn, bodyEn, resourceId);
        saveAndPushSingle(notificationFactory.apply(message.toContent()), userId, message);
    }

    /** 페스티벌 D-day 리마인더 (스케줄러에서 호출) */
    public void sendFestivalReminders(Long festivalId, String festivalTitle, String festivalTitleEn,
                                       List<Long> userIds, int dDay) {
        if (userIds.isEmpty()) return;

        String title = NotificationMessages.festivalReminderTitle(dDay);
        String body = NotificationMessages.festivalReminderBody(festivalTitle, dDay);
        String titleEn = NotificationMessages.festivalReminderTitleEn(dDay);
        String bodyEn = NotificationMessages.festivalReminderBodyEn(festivalTitleEn, dDay);

        Festival festival = festivalRepository.findById(festivalId).orElse(null);
        List<User> users = userRepository.findAllById(userIds);
        NotificationMessage message = new NotificationMessage(
                NotificationType.FESTIVAL_REMINDER, title, body, titleEn, bodyEn, String.valueOf(festivalId));
        saveAndPush(users, message, festival);
        log.info("[Notification] D-{} 리마인더 {}건 발송 (festivalId={})", dDay, users.size(), festivalId);
    }

    private void saveAndPushSingle(Notification notification, Long userId, NotificationMessage message) {
        notificationRepository.save(notification);
        pushIfEnabled(userId, message);
    }

    private void pushIfEnabled(Long userId, NotificationMessage message) {
        NotificationPreference pref = preferenceService.getOrCreate(userId);
        if (!pref.isEnabledFor(message.type())) return;
        List<TokenLanguageProjection> tokens =
                deviceTokenRepository.findTokensWithLanguageByUserIds(List.of(userId));
        sendByLanguage(tokens, message);
    }

    private void saveAndPush(List<User> users, NotificationMessage message, Festival festival) {
        notificationRepository.saveAll(users.stream()
                .map(u -> Notification.of(u, message.toContent(), festival))
                .toList());
        List<Long> allUserIds = users.stream().map(User::getId).toList();
        Map<Long, NotificationPreference> prefMap = preferenceService.getOrCreateBatch(allUserIds);
        List<Long> enabledUserIds = allUserIds.stream()
                .filter(id -> prefMap.get(id).isEnabledFor(message.type()))
                .toList();
        List<TokenLanguageProjection> tokens =
                deviceTokenRepository.findTokensWithLanguageByUserIds(enabledUserIds);
        sendByLanguage(tokens, message);
    }

    private void sendByLanguage(List<TokenLanguageProjection> tokens, NotificationMessage message) {
        Map<String, List<String>> byLang = tokens.stream()
                .collect(Collectors.groupingBy(
                        t -> "en".equals(t.getLanguage()) ? "en" : "ko",
                        Collectors.mapping(TokenLanguageProjection::getToken, Collectors.toList())
                ));
        List<String> koTokens = byLang.getOrDefault("ko", List.of());
        List<String> enTokens = byLang.getOrDefault("en", List.of());
        if (!koTokens.isEmpty()) {
            fcmPushService.sendMulticast(koTokens, new PushMessage(message.title(), message.body(), message.resourceId(), message.type()));
        }
        if (!enTokens.isEmpty()) {
            fcmPushService.sendMulticast(enTokens, new PushMessage(message.titleEn(), message.bodyEn(), message.resourceId(), message.type()));
        }
    }

}
