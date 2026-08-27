package com.feple.feple_backend.notification.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artist.song.event.SongRequestApprovedEvent;
import com.feple.feple_backend.artist.song.event.SongRequestRejectedEvent;
import com.feple.feple_backend.artist.suggestion.event.ArtistSuggestionProcessedEvent;
import com.feple.feple_backend.artistfestival.event.ArtistAddedToFestivalEvent;
import com.feple.feple_backend.artistfollow.entity.ArtistFollow;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.certification.event.CertificationApprovedEvent;
import com.feple.feple_backend.certification.event.CertificationRejectedEvent;
import com.feple.feple_backend.comment.event.CommentCreatedEvent;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.festival.suggestion.event.FestivalSuggestionProcessedEvent;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.global.KoreaClock;
import com.feple.feple_backend.global.exception.InvalidRequestException;
import com.feple.feple_backend.notification.entity.Notification;
import com.feple.feple_backend.notification.entity.NotificationContent;
import com.feple.feple_backend.notification.entity.NotificationPreference;
import com.feple.feple_backend.notification.entity.NotificationType;
import com.feple.feple_backend.notification.entity.PendingPush;
import com.feple.feple_backend.notification.entity.PreferenceCategory;
import com.feple.feple_backend.notification.repository.NotificationRepository;
import com.feple.feple_backend.notification.repository.PendingPushRepository;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.event.PostDeletedByAdminEvent;
import com.feple.feple_backend.post.event.PostLikedEvent;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.event.AdminPointGrantedEvent;
import com.feple.feple_backend.user.repository.UserDeviceTokenRepository;
import com.feple.feple_backend.user.repository.UserDeviceTokenRepository.TokenLanguageProjection;
import com.feple.feple_backend.user.repository.UserRepository;
import com.feple.feple_backend.userblock.service.UserBlockService;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    /** 심야 알림 차단 시간대(KST) — 사용자가 설정에서 켠 경우에만 적용, 관리자 수동 발송(AdminPushService)은 이 게이트를 거치지 않는다 */
    private static final LocalTime QUIET_HOURS_START = LocalTime.MIDNIGHT;
    private static final LocalTime QUIET_HOURS_END = LocalTime.of(7, 0);

    /** 댓글/좋아요를 제외한 자동 알림은 00:00~09:00(KST)에 발생하면 즉시 보내지 않고 대기열에 쌓았다가 오전 9시에 발송 */
    private static final LocalTime MORNING_DELIVERY_TIME = LocalTime.of(9, 0);

    /** 대량 팬아웃 알림을 한 번에 처리하는 사용자 수 상한 (FCM multicast 상한과 동일) */
    private static final int FAN_OUT_CHUNK_SIZE = 500;

    private final NotificationRepository notificationRepository;
    private final PendingPushRepository pendingPushRepository;
    private final ArtistFollowRepository artistFollowRepository;
    private final ArtistRepository artistRepository;
    private final UserDeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;
    private final FestivalRepository festivalRepository;
    private final PostRepository postRepository;
    private final PushNotificationClient fcmPushService;
    private final NotificationPreferenceService preferenceService;
    private final UserBlockService userBlockService;
    private final FileStorageService fileStorageService;

    private record NotificationMessage(NotificationType type, String title, String body,
                                        String titleEn, String bodyEn, String resourceId, String imageUrl) {
        // 기존 호출부(이미지 없음)와의 호환용 — imageUrl은 notifySingle/saveAndPush/sendFestivalReminders에서
        // 엔티티의 포스터/프로필 이미지 키를 resolveImageUrl()로 채워 넣는다.
        NotificationMessage(NotificationType type, String title, String body,
                             String titleEn, String bodyEn, String resourceId) {
            this(type, title, body, titleEn, bodyEn, resourceId, null);
        }

        NotificationContent toContent() {
            return new NotificationContent(type, title, body, titleEn, bodyEn);
        }

        NotificationMessage withImageUrl(String imageUrl) {
            return new NotificationMessage(type, title, body, titleEn, bodyEn, resourceId, imageUrl);
        }
    }

    private User findUserOrNull(Long userId) {
        return userRepository.findById(userId).orElse(null);
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

        NotificationMessage message = new NotificationMessage(
                NotificationType.NEW_FESTIVAL, title, body, titleEn, bodyEn, String.valueOf(event.festivalId()));
        fanOut(userIds, message, festival);
        log.info("[Notification] 인앱 알림 {}건 저장 (artistId={}, festivalId={})", userIds.size(), event.artistId(), event.festivalId());
    }

    /** 인증 승인 알림 — 커밋 후에만 발송 */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCertificationApproved(CertificationApprovedEvent event) {
        User user = findUserOrNull(event.userId());
        if (user == null) return;
        Festival festival = festivalRepository.findById(event.festivalId()).orElse(null);
        notifySingle(event.userId(), new NotificationMessage(NotificationType.CERT_APPROVED,
                        NotificationMessages.CERT_APPROVED_TITLE,
                        NotificationMessages.certApprovedBody(event.festivalTitle()),
                        NotificationMessages.CERT_APPROVED_TITLE_EN,
                        NotificationMessages.certApprovedBodyEn(event.festivalTitleEn()),
                        String.valueOf(event.festivalId())),
                content -> Notification.of(user, content, festival));
    }

    /** 인증 거절 알림 — 커밋 후에만 발송 */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCertificationRejected(CertificationRejectedEvent event) {
        User user = findUserOrNull(event.userId());
        if (user == null) return;
        Festival festival = festivalRepository.findById(event.festivalId()).orElse(null);
        notifySingle(event.userId(), new NotificationMessage(NotificationType.CERT_REJECTED,
                        NotificationMessages.CERT_REJECTED_TITLE,
                        NotificationMessages.certRejectedBody(event.festivalTitle(), event.reason()),
                        NotificationMessages.CERT_REJECTED_TITLE_EN,
                        NotificationMessages.certRejectedBodyEn(event.festivalTitleEn(), event.reason()),
                        String.valueOf(event.festivalId())),
                content -> Notification.of(user, content, festival));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSongRequestApproved(SongRequestApprovedEvent event) {
        User user = findUserOrNull(event.userId());
        if (user == null) return;
        Artist artist = artistRepository.findById(event.artistId()).orElse(null);
        notifySingle(event.userId(), new NotificationMessage(NotificationType.SONG_REQUEST_APPROVED,
                        NotificationMessages.SONG_REQUEST_APPROVED_TITLE,
                        NotificationMessages.songRequestApprovedBody(event.songTitle(), event.artistName()),
                        NotificationMessages.SONG_REQUEST_APPROVED_TITLE_EN,
                        NotificationMessages.songRequestApprovedBodyEn(event.songTitle(), event.artistNameEn()),
                        String.valueOf(event.artistId())),
                content -> Notification.of(user, content, artist));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSongRequestRejected(SongRequestRejectedEvent event) {
        User user = findUserOrNull(event.userId());
        if (user == null) return;
        Artist artist = artistRepository.findById(event.artistId()).orElse(null);
        notifySingle(event.userId(), new NotificationMessage(NotificationType.SONG_REQUEST_REJECTED,
                        NotificationMessages.SONG_REQUEST_REJECTED_TITLE,
                        NotificationMessages.songRequestRejectedBody(event.songTitle(), event.reason()),
                        NotificationMessages.SONG_REQUEST_REJECTED_TITLE_EN,
                        NotificationMessages.songRequestRejectedBodyEn(event.songTitle(), event.reason()),
                        String.valueOf(event.artistId())),
                content -> Notification.of(user, content, artist));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onArtistSuggestionProcessed(ArtistSuggestionProcessedEvent event) {
        User user = findUserOrNull(event.userId());
        if (user == null) return;
        String resourceId = event.artistId() != null ? String.valueOf(event.artistId()) : null;
        Artist artist = event.artistId() != null ? artistRepository.findById(event.artistId()).orElse(null) : null;
        String artistNameEn = (artist != null && artist.getNameEn() != null && !artist.getNameEn().isBlank())
                ? artist.getNameEn() : event.artistName();
        notifySingle(event.userId(), new NotificationMessage(NotificationType.ARTIST_SUGGESTION_PROCESSED,
                        NotificationMessages.ARTIST_SUGGESTION_PROCESSED_TITLE,
                        NotificationMessages.artistSuggestionProcessedBody(event.artistName(), event.note()),
                        NotificationMessages.ARTIST_SUGGESTION_PROCESSED_TITLE_EN,
                        NotificationMessages.artistSuggestionProcessedBodyEn(artistNameEn, event.note()),
                        resourceId),
                content -> artist != null ? Notification.of(user, content, artist) : Notification.of(user, content));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFestivalSuggestionProcessed(FestivalSuggestionProcessedEvent event) {
        User user = findUserOrNull(event.userId());
        if (user == null) return;
        String resourceId = event.festivalId() != null ? String.valueOf(event.festivalId()) : null;
        Festival festival = event.festivalId() != null ? festivalRepository.findById(event.festivalId()).orElse(null) : null;
        String festivalNameEn = (festival != null && festival.getTitleEn() != null && !festival.getTitleEn().isBlank())
                ? festival.getTitleEn() : event.festivalName();
        notifySingle(event.userId(), new NotificationMessage(NotificationType.FESTIVAL_SUGGESTION_PROCESSED,
                        NotificationMessages.FESTIVAL_SUGGESTION_PROCESSED_TITLE,
                        NotificationMessages.festivalSuggestionProcessedBody(event.festivalName(), event.note()),
                        NotificationMessages.FESTIVAL_SUGGESTION_PROCESSED_TITLE_EN,
                        NotificationMessages.festivalSuggestionProcessedBodyEn(festivalNameEn, event.note()),
                        resourceId),
                content -> festival != null ? Notification.of(user, content, festival) : Notification.of(user, content));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentCreated(CommentCreatedEvent event) {
        if (event.postAuthorId() != null && !userBlockService.isBlocked(event.postAuthorId(), event.commenterId())) {
            notifyNewComment(event.postAuthorId(), event);
        }
        if (event.mentionedUserId() != null && !userBlockService.isBlocked(event.mentionedUserId(), event.commenterId())) {
            notifyNewReply(event.mentionedUserId(), event);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostLiked(PostLikedEvent event) {
        if (userBlockService.isBlocked(event.postAuthorId(), event.likerId())) return;
        User author = findUserOrNull(event.postAuthorId());
        if (author == null) return;
        Post post = postRepository.findById(event.postId()).orElse(null);
        notifySingle(event.postAuthorId(), new NotificationMessage(NotificationType.POST_LIKED,
                        NotificationMessages.postLikedTitle(event.likerNickname()),
                        NotificationMessages.postLikedBody(event.postTitle()),
                        NotificationMessages.postLikedTitleEn(event.likerNickname()),
                        NotificationMessages.postLikedBodyEn(event.postTitle()),
                        String.valueOf(event.postId())),
                content -> Notification.of(author, content, post));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostDeletedByAdmin(PostDeletedByAdminEvent event) {
        User author = findUserOrNull(event.postAuthorId());
        if (author == null) return;
        notifySingle(event.postAuthorId(), new NotificationMessage(NotificationType.POST_DELETED_BY_ADMIN,
                        NotificationMessages.POST_DELETED_BY_ADMIN_TITLE,
                        NotificationMessages.postDeletedByAdminBody(event.postTitle()),
                        NotificationMessages.POST_DELETED_BY_ADMIN_TITLE_EN,
                        NotificationMessages.postDeletedByAdminBodyEn(event.postTitle()),
                        null),
                content -> Notification.of(author, content));
    }

    /** 관리자 수동 포인트 지급 알림 — 커밋 후에만 발송 */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAdminPointGranted(AdminPointGrantedEvent event) {
        User user = findUserOrNull(event.userId());
        if (user == null) return;
        notifySingle(event.userId(), new NotificationMessage(NotificationType.ADMIN_POINT_GRANTED,
                        NotificationMessages.adminPointGrantedTitle(event.amount()),
                        NotificationMessages.adminPointGrantedBody(event.reason()),
                        NotificationMessages.adminPointGrantedTitleEn(event.amount()),
                        NotificationMessages.adminPointGrantedBodyEn(event.amount()),
                        null),
                content -> Notification.of(user, content));
    }

    /** 내 게시글에 댓글 알림 — onCommentCreated에서만 호출 (자체 호출이라 별도 @Async/@Transactional 불필요) */
    private void notifyNewComment(Long postAuthorId, CommentCreatedEvent event) {
        // 자기 자신의 댓글이면 알림 없음
        User author = findUserOrNull(postAuthorId);
        if (author == null) return;

        String commenterNickname = event.commenterNickname();
        String postTitle = event.postTitle();
        Post post = postRepository.findById(event.postId()).orElse(null);
        notifySingle(postAuthorId, new NotificationMessage(NotificationType.NEW_COMMENT,
                        NotificationMessages.newCommentTitle(commenterNickname),
                        NotificationMessages.newCommentBody(postTitle),
                        NotificationMessages.newCommentTitleEn(commenterNickname),
                        NotificationMessages.newCommentBodyEn(postTitle),
                        null),
                content -> Notification.of(author, content, post));
    }

    /** 내 댓글에 대댓글 알림 — onCommentCreated에서만 호출 (자체 호출이라 별도 @Async/@Transactional 불필요) */
    private void notifyNewReply(Long mentionedUserId, CommentCreatedEvent event) {
        User author = findUserOrNull(mentionedUserId);
        if (author == null) return;
        String replierNickname = event.commenterNickname();
        String postTitle = event.postTitle();
        Post post = postRepository.findById(event.postId()).orElse(null);
        notifySingle(mentionedUserId, new NotificationMessage(NotificationType.NEW_REPLY,
                        NotificationMessages.newReplyTitle(replierNickname),
                        NotificationMessages.newReplyBody(postTitle),
                        NotificationMessages.newReplyTitleEn(replierNickname),
                        NotificationMessages.newReplyBodyEn(postTitle),
                        null),
                content -> Notification.of(author, content, post));
    }

    private void notifySingle(Long userId, NotificationMessage message,
                               Function<NotificationContent, Notification> notificationFactory) {
        Notification notification = notificationFactory.apply(message.toContent());
        NotificationMessage messageWithImage = resolveImage(message, notification.getImageKey());
        saveAndPushSingle(notification, userId, messageWithImage);
    }

    /** 알림에 딸린 포스터/프로필 이미지 키를 공개 URL로 변환해 FCM payload에 실을 수 있게 한다 */
    private NotificationMessage resolveImage(NotificationMessage message, String imageKey) {
        if (imageKey == null) return message;
        return message.withImageUrl(fileStorageService.buildUrl(imageKey));
    }

    /** 관리자 테스트 발송용 개별 알림 저장 (AdminPushService에서 호출) */
    public void saveAdminBroadcastNotification(Long userId, String title, String body) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidRequestException("사용자를 찾을 수 없습니다. (userId=" + userId + ")"));
        notificationRepository.save(Notification.of(
                user, new NotificationContent(NotificationType.ADMIN_BROADCAST, title, body, null, null)));
    }

    /** 관리자 타겟 발송(아티스트 팔로워/페스티벌 인증자)용 개별 알림 일괄 저장 (AdminPushService에서 호출) */
    public void saveAdminBroadcastNotifications(List<Long> userIds, String title, String body) {
        List<User> users = userRepository.findAllById(userIds);
        notificationRepository.saveAll(users.stream()
                .map(u -> Notification.of(u, new NotificationContent(NotificationType.ADMIN_BROADCAST, title, body, null, null)))
                .toList());
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
        NotificationMessage message = new NotificationMessage(
                NotificationType.FESTIVAL_REMINDER, title, body, titleEn, bodyEn, String.valueOf(festivalId));
        fanOut(userIds, message, festival);
        log.info("[Notification] D-{} 리마인더 {}건 발송 (festivalId={})", dDay, userIds.size(), festivalId);
    }

    // 팔로워/참석자 대량 알림은 청크로 나눠 저장·발송한다 — findAllById·saveAll·IN 쿼리가
    // 한 번에 수만 건을 다루지 않도록.
    private void fanOut(List<Long> userIds, NotificationMessage message, Festival festival) {
        for (int i = 0; i < userIds.size(); i += FAN_OUT_CHUNK_SIZE) {
            List<Long> chunk = userIds.subList(i, Math.min(i + FAN_OUT_CHUNK_SIZE, userIds.size()));
            saveAndPush(userRepository.findAllById(chunk), message, festival);
        }
    }

    private void saveAndPushSingle(Notification notification, Long userId, NotificationMessage message) {
        notificationRepository.save(notification);
        pushIfEnabled(userId, message);
    }

    private void pushIfEnabled(Long userId, NotificationMessage message) {
        NotificationPreference pref = preferenceService.getOrCreate(userId);
        if (!pref.isEnabledFor(message.type()) || isBlockedByQuietHours(pref)) return;
        pushOrDeferToMorning(message, List.of(userId));
    }

    private void saveAndPush(List<User> users, NotificationMessage message, Festival festival) {
        notificationRepository.saveAll(users.stream()
                .map(u -> Notification.of(u, message.toContent(), festival))
                .toList());
        NotificationMessage messageWithImage =
                resolveImage(message, festival != null ? festival.getPosterKey() : null);
        List<Long> allUserIds = users.stream().map(User::getId).toList();
        Map<Long, NotificationPreference> prefMap = preferenceService.getOrCreateBatch(allUserIds);
        List<Long> enabledUserIds = allUserIds.stream()
                .filter(id -> prefMap.get(id).isEnabledFor(message.type()) && !isBlockedByQuietHours(prefMap.get(id)))
                .toList();
        pushOrDeferToMorning(messageWithImage, enabledUserIds);
    }

    private boolean isBlockedByQuietHours(NotificationPreference pref) {
        if (!pref.isQuietHoursEnabled()) return false;
        LocalTime now = KoreaClock.now();
        return !now.isBefore(QUIET_HOURS_START) && now.isBefore(QUIET_HOURS_END);
    }

    /** 대상이 없으면 무시, 새벽 배송 대상이면 대기열에 적재, 아니면 즉시 발송 */
    private void pushOrDeferToMorning(NotificationMessage message, List<Long> userIds) {
        if (userIds.isEmpty()) return;
        if (shouldDeferToMorning(message.type())) {
            enqueuePendingPush(message, userIds);
            return;
        }
        List<TokenLanguageProjection> tokens = deviceTokenRepository.findTokensWithLanguageByUserIds(userIds);
        sendByLanguage(tokens, message);
    }

    /** 댓글/좋아요(PreferenceCategory.COMMENT)는 사용자 상호작용에 대한 즉각적인 반응이라 예외 — 그 외 자동 알림만 새벽에 지연 */
    private boolean shouldDeferToMorning(NotificationType type) {
        return type.getCategory() != PreferenceCategory.COMMENT && KoreaClock.now().isBefore(MORNING_DELIVERY_TIME);
    }

    private void enqueuePendingPush(NotificationMessage message, List<Long> userIds) {
        pendingPushRepository.save(PendingPush.builder()
                .type(message.type())
                .title(message.title())
                .body(message.body())
                .titleEn(message.titleEn())
                .bodyEn(message.bodyEn())
                .resourceId(message.resourceId())
                .imageUrl(message.imageUrl())
                .userIds(userIds)
                .build());
    }

    /**
     * 매일 오전 9시(KST) PendingPushScheduler가 호출 — 밤사이 쌓인 대기열을 발송한다.
     * 전체를 하나의 트랜잭션으로 묶지 않는다: 항목마다 외부 FCM 호출이 있어, 묶으면 배치 전체
     * 소요 시간 동안 DB 커넥션을 점유한다. 조회는 한 번에(수신자까지 fetch), 발송은 트랜잭션
     * 없이, 삭제는 항목별 독립 트랜잭션으로 처리한다.
     */
    public void flushPendingPushes() {
        List<PendingPush> pending = pendingPushRepository.findAllWithRecipients();
        for (PendingPush p : pending) {
            try {
                dispatchPendingPush(p);
                pendingPushRepository.delete(p);
            } catch (Exception e) {
                log.error("[PendingPush] 발송 실패 id={}", p.getId(), e);
            }
        }
    }

    private void dispatchPendingPush(PendingPush p) {
        List<TokenLanguageProjection> tokens =
                deviceTokenRepository.findTokensWithLanguageByUserIds(p.getUserIds());
        NotificationMessage message = new NotificationMessage(
                p.getType(), p.getTitle(), p.getBody(), p.getTitleEn(), p.getBodyEn(), p.getResourceId(), p.getImageUrl());
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
            fcmPushService.sendMulticast(koTokens, new PushMessage(
                    message.title(), message.body(), message.resourceId(), message.type(), message.imageUrl()));
        }
        if (!enTokens.isEmpty()) {
            fcmPushService.sendMulticast(enTokens, new PushMessage(
                    message.titleEn(), message.bodyEn(), message.resourceId(), message.type(), message.imageUrl()));
        }
    }

}
