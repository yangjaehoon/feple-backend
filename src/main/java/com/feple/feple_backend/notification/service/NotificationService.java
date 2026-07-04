package com.feple.feple_backend.notification.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
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
import com.feple.feple_backend.artistfestival.event.ArtistAddedToFestivalEvent;
import com.feple.feple_backend.certification.event.CertificationApprovedEvent;
import com.feple.feple_backend.certification.event.CertificationRejectedEvent;
import com.feple.feple_backend.comment.event.CommentCreatedEvent;
import com.feple.feple_backend.post.event.PostLikedEvent;
import com.feple.feple_backend.post.event.PostDeletedByAdminEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.feple.feple_backend.user.repository.UserDeviceTokenRepository.TokenLanguageProjection;
import java.util.List;
import java.util.Map;
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

    /** 아티스트가 페스티벌에 추가될 때 팔로워들에게 알림 발송 — 커밋 후에만 발송 */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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

        saveAndPush(users, NotificationType.NEW_FESTIVAL, title, body, titleEn, bodyEn, festival, String.valueOf(event.festivalId()));
        log.info("[Notification] 인앱 알림 {}건 저장 (artistId={}, festivalId={})", users.size(), event.artistId(), event.festivalId());
    }

    /** 인증 승인 알림 — 커밋 후에만 발송 */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCertificationApproved(CertificationApprovedEvent event) {
        User user = userRepository.findById(event.userId()).orElse(null);
        if (user == null) return;
        Festival festival = festivalRepository.findById(event.festivalId()).orElse(null);
        String title = NotificationMessages.CERT_APPROVED_TITLE;
        String body = NotificationMessages.certApprovedBody(event.festivalTitle());
        String titleEn = NotificationMessages.CERT_APPROVED_TITLE_EN;
        String bodyEn = NotificationMessages.certApprovedBodyEn(event.festivalTitleEn());
        notificationRepository.save(Notification.of(user, NotificationType.CERT_APPROVED, title, body, titleEn, bodyEn, festival));
        maybePush(event.userId(), NotificationType.CERT_APPROVED, title, body, titleEn, bodyEn, String.valueOf(event.festivalId()));
    }

    /** 인증 거절 알림 — 커밋 후에만 발송 */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCertificationRejected(CertificationRejectedEvent event) {
        User user = userRepository.findById(event.userId()).orElse(null);
        if (user == null) return;
        Festival festival = festivalRepository.findById(event.festivalId()).orElse(null);
        String title = NotificationMessages.CERT_REJECTED_TITLE;
        String body = NotificationMessages.certRejectedBody(event.festivalTitle(), event.reason());
        String titleEn = NotificationMessages.CERT_REJECTED_TITLE_EN;
        String bodyEn = NotificationMessages.certRejectedBodyEn(event.festivalTitleEn(), event.reason());
        notificationRepository.save(Notification.of(user, NotificationType.CERT_REJECTED, title, body, titleEn, bodyEn, festival));
        maybePush(event.userId(), NotificationType.CERT_REJECTED, title, body, titleEn, bodyEn, String.valueOf(event.festivalId()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onSongRequestApproved(SongRequestApprovedEvent event) {
        User user = userRepository.findById(event.userId()).orElse(null);
        if (user == null) return;
        Artist artist = artistRepository.findById(event.artistId()).orElse(null);
        String title = NotificationMessages.SONG_REQUEST_APPROVED_TITLE;
        String body = NotificationMessages.songRequestApprovedBody(event.songTitle(), event.artistName());
        String titleEn = NotificationMessages.SONG_REQUEST_APPROVED_TITLE_EN;
        String bodyEn = NotificationMessages.songRequestApprovedBodyEn(event.songTitle(), event.artistNameEn());
        notificationRepository.save(
                Notification.of(user, NotificationType.SONG_REQUEST_APPROVED, title, body, titleEn, bodyEn, artist));
        maybePush(event.userId(), NotificationType.SONG_REQUEST_APPROVED, title, body, titleEn, bodyEn, String.valueOf(event.artistId()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onSongRequestRejected(SongRequestRejectedEvent event) {
        User user = userRepository.findById(event.userId()).orElse(null);
        if (user == null) return;
        Artist artist = artistRepository.findById(event.artistId()).orElse(null);
        String title = NotificationMessages.SONG_REQUEST_REJECTED_TITLE;
        String body = NotificationMessages.songRequestRejectedBody(event.songTitle(), event.reason());
        String titleEn = NotificationMessages.SONG_REQUEST_REJECTED_TITLE_EN;
        String bodyEn = NotificationMessages.songRequestRejectedBodyEn(event.songTitle(), event.reason());
        notificationRepository.save(
                Notification.of(user, NotificationType.SONG_REQUEST_REJECTED, title, body, titleEn, bodyEn, artist));
        maybePush(event.userId(), NotificationType.SONG_REQUEST_REJECTED, title, body, titleEn, bodyEn, String.valueOf(event.artistId()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onArtistSuggestionProcessed(ArtistSuggestionProcessedEvent event) {
        User user = userRepository.findById(event.userId()).orElse(null);
        if (user == null) return;
        String title = NotificationMessages.ARTIST_SUGGESTION_PROCESSED_TITLE;
        String body = NotificationMessages.artistSuggestionProcessedBody(event.artistName(), event.note());
        String titleEn = NotificationMessages.ARTIST_SUGGESTION_PROCESSED_TITLE_EN;
        String linkId = event.artistId() != null ? String.valueOf(event.artistId()) : null;
        Artist artist = event.artistId() != null ? artistRepository.findById(event.artistId()).orElse(null) : null;
        String artistNameEn = (artist != null && artist.getNameEn() != null && !artist.getNameEn().isBlank())
                ? artist.getNameEn() : event.artistName();
        String bodyEn = NotificationMessages.artistSuggestionProcessedBodyEn(artistNameEn, event.note());
        if (artist != null) {
            notificationRepository.save(
                    Notification.of(user, NotificationType.ARTIST_SUGGESTION_PROCESSED, title, body, titleEn, bodyEn, artist));
        } else {
            notificationRepository.save(
                    Notification.of(user, NotificationType.ARTIST_SUGGESTION_PROCESSED, title, body, titleEn, bodyEn, (Festival) null));
        }
        maybePush(event.userId(), NotificationType.ARTIST_SUGGESTION_PROCESSED, title, body, titleEn, bodyEn, linkId);
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
        User author = userRepository.findById(event.postAuthorId()).orElse(null);
        if (author == null) return;
        String title = NotificationMessages.postLikedTitle(event.likerNickname());
        String body = NotificationMessages.postLikedBody(event.postTitle());
        String titleEn = NotificationMessages.postLikedTitleEn(event.likerNickname());
        String bodyEn = NotificationMessages.postLikedBodyEn(event.postTitle());
        Post post = postRepository.findById(event.postId()).orElse(null);
        notificationRepository.save(
                Notification.of(author, NotificationType.POST_LIKED, title, body, titleEn, bodyEn, post));
        maybePush(event.postAuthorId(), NotificationType.POST_LIKED, title, body, titleEn, bodyEn, String.valueOf(event.postId()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPostDeletedByAdmin(PostDeletedByAdminEvent event) {
        User author = userRepository.findById(event.postAuthorId()).orElse(null);
        if (author == null) return;
        String title = NotificationMessages.POST_DELETED_BY_ADMIN_TITLE;
        String body = NotificationMessages.postDeletedByAdminBody(event.postTitle());
        String titleEn = NotificationMessages.POST_DELETED_BY_ADMIN_TITLE_EN;
        String bodyEn = NotificationMessages.postDeletedByAdminBodyEn(event.postTitle());
        notificationRepository.save(
                Notification.of(author, NotificationType.POST_DELETED_BY_ADMIN, title, body, titleEn, bodyEn, (Festival) null));
        maybePush(event.postAuthorId(), NotificationType.POST_DELETED_BY_ADMIN, title, body, titleEn, bodyEn, null);
    }

    /** 내 게시글에 댓글 알림 — onCommentCreated에서만 호출 (자체 호출이라 별도 @Async/@Transactional 불필요) */
    private void notifyNewComment(Long postAuthorId, String commenterNickname,
                                  String postTitle, Long postId) {
        // 자기 자신의 댓글이면 알림 없음
        User author = userRepository.findById(postAuthorId).orElse(null);
        if (author == null) return;

        String title = NotificationMessages.newCommentTitle(commenterNickname);
        String body = NotificationMessages.newCommentBody(postTitle);
        String titleEn = NotificationMessages.newCommentTitleEn(commenterNickname);
        String bodyEn = NotificationMessages.newCommentBodyEn(postTitle);

        Post post = postRepository.findById(postId).orElse(null);
        notificationRepository.save(
                Notification.of(author, NotificationType.NEW_COMMENT, title, body, titleEn, bodyEn, post));
        maybePush(postAuthorId, NotificationType.NEW_COMMENT, title, body, titleEn, bodyEn, null);
    }

    /** 내 댓글에 대댓글 알림 — onCommentCreated에서만 호출 (자체 호출이라 별도 @Async/@Transactional 불필요) */
    private void notifyNewReply(Long parentCommentAuthorId, String replierNickname,
                                String postTitle, Long postId) {
        User author = userRepository.findById(parentCommentAuthorId).orElse(null);
        if (author == null) return;
        String title = NotificationMessages.newReplyTitle(replierNickname);
        String body = NotificationMessages.newReplyBody(postTitle);
        String titleEn = NotificationMessages.newReplyTitleEn(replierNickname);
        String bodyEn = NotificationMessages.newReplyBodyEn(postTitle);
        Post post = postRepository.findById(postId).orElse(null);
        notificationRepository.save(
                Notification.of(author, NotificationType.NEW_REPLY, title, body, titleEn, bodyEn, post));
        maybePush(parentCommentAuthorId, NotificationType.NEW_REPLY, title, body, titleEn, bodyEn, null);
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

    private void maybePush(Long userId, NotificationType type,
                            String title, String body, String titleEn, String bodyEn, String linkId) {
        NotificationPreference pref = preferenceService.getOrCreate(userId);
        if (!pref.isEnabledFor(type)) return;
        List<TokenLanguageProjection> tokens =
                deviceTokenRepository.findTokensWithLanguageByUserIds(List.of(userId));
        sendByLanguage(tokens, title, body, titleEn, bodyEn, linkId, type);
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
        List<TokenLanguageProjection> tokens =
                deviceTokenRepository.findTokensWithLanguageByUserIds(enabledUserIds);
        sendByLanguage(tokens, title, body, titleEn, bodyEn, linkId, type);
    }

    private void sendByLanguage(List<TokenLanguageProjection> tokens,
                                 String title, String body, String titleEn, String bodyEn,
                                 String linkId, NotificationType type) {
        Map<String, List<String>> byLang = tokens.stream()
                .collect(Collectors.groupingBy(
                        t -> "en".equals(t.getLanguage()) ? "en" : "ko",
                        Collectors.mapping(TokenLanguageProjection::getToken, Collectors.toList())
                ));
        List<String> koTokens = byLang.getOrDefault("ko", List.of());
        List<String> enTokens = byLang.getOrDefault("en", List.of());
        if (!koTokens.isEmpty()) fcmPushService.sendMulticast(koTokens, title, body, linkId, type);
        if (!enTokens.isEmpty()) fcmPushService.sendMulticast(enTokens, titleEn, bodyEn, linkId, type);
    }

}
