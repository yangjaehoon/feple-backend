package com.feple.feple_backend.notification.service;

import com.feple.feple_backend.file.service.S3PresignService;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.global.OwnershipValidator;
import com.feple.feple_backend.notification.dto.NotificationDto;
import com.feple.feple_backend.notification.entity.Notification;
import com.feple.feple_backend.notification.entity.NotificationType;
import com.feple.feple_backend.notification.repository.BroadcastNotificationRepository;
import com.feple.feple_backend.notification.repository.NotificationRepository;
import com.feple.feple_backend.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryService {

    private static final int MAX_PERSONAL = 200;
    private static final int MAX_BROADCAST = 50;

    private static final Set<NotificationType> CERT_TYPES = EnumSet.of(
            NotificationType.CERT_APPROVED, NotificationType.CERT_REJECTED);

    private static final Set<NotificationType> COMMENT_TYPES = EnumSet.of(
            NotificationType.NEW_COMMENT, NotificationType.NEW_REPLY,
            NotificationType.POST_LIKED, NotificationType.POST_DELETED_BY_ADMIN);

    private static final Set<NotificationType> FESTIVAL_TYPES = EnumSet.of(
            NotificationType.NEW_FESTIVAL, NotificationType.FESTIVAL_REMINDER,
            NotificationType.SONG_REQUEST_APPROVED, NotificationType.SONG_REQUEST_REJECTED,
            NotificationType.ARTIST_SUGGESTION_PROCESSED, NotificationType.FESTIVAL_SUGGESTION_PROCESSED);

    private final NotificationRepository notificationRepository;
    private final BroadcastNotificationRepository broadcastNotificationRepository;
    private final UserRepository userRepository;
    private final S3PresignService s3PresignService;

    public Page<NotificationDto> getMyNotifications(Long userId, Pageable pageable, String typeGroup) {
        Set<NotificationType> typeFilter = resolveTypeFilter(typeGroup);
        List<MergedNotification> all = fetchMergedNotifications(userId, typeFilter);
        return paginate(all, pageable);
    }

    // presign은 최종 페이지에 실제로 노출되는 항목에 대해서만 수행해야 한다 — merge 단계에서
    // 최대 250건(개인 200+공지 50) 전부에 대해 미리 presign하면 대부분 버려지는 낭비 작업이 된다.
    private record MergedNotification(NotificationDto dto, String imageKey) {}

    private List<MergedNotification> fetchMergedNotifications(Long userId, Set<NotificationType> typeFilter) {
        // 타입 필터가 있으면 쿼리 단계에서 바로 걸러낸다 — 최신 N건을 먼저 자른 뒤 타입으로
        // 거르면, 그 N건이 특정 타입에 편중된 경우 결과가 실제보다 적게 나올 수 있다.
        if (typeFilter != null) {
            return notificationRepository
                    .findByUserIdAndTypeInOrderByCreatedAtDesc(userId, typeFilter, PageRequest.of(0, MAX_PERSONAL))
                    .stream()
                    .map(this::toMergedPersonal)
                    .sorted(Comparator.comparing(m -> m.dto().createdAt(), Comparator.reverseOrder()))
                    .toList();
        }

        List<MergedNotification> personal = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, MAX_PERSONAL))
                .stream()
                .map(this::toMergedPersonal)
                .toList();
        // 가입 이전에 발송된 전체 공지는 신규 유저에게 새 알림처럼 보이면 안 되므로 가입일 이후 것만 병합한다.
        LocalDateTime joinedAt = userRepository.findCreatedAtById(userId).orElse(LocalDateTime.MIN);
        List<MergedNotification> broadcasts = broadcastNotificationRepository
                .findByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(joinedAt, PageRequest.of(0, MAX_BROADCAST))
                .stream()
                .map(b -> new MergedNotification(NotificationDto.forBroadcast(b), null))
                .toList();

        return Stream.concat(personal.stream(), broadcasts.stream())
                .sorted(Comparator.comparing(m -> m.dto().createdAt(), Comparator.reverseOrder()))
                .toList();
    }

    private MergedNotification toMergedPersonal(Notification n) {
        return new MergedNotification(NotificationDto.from(n, null), n.getImageKey());
    }

    private Page<NotificationDto> paginate(List<MergedNotification> all, Pageable pageable) {
        int total = all.size();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), total);
        List<NotificationDto> paged = start >= total
                ? List.of()
                : all.subList(start, end).stream().map(this::resolveImage).toList();
        return new PageImpl<>(paged, pageable, total);
    }

    private NotificationDto resolveImage(MergedNotification merged) {
        if (merged.imageKey() == null) return merged.dto();
        NotificationDto dto = merged.dto();
        return new NotificationDto(dto.id(), dto.type(), dto.title(), dto.body(), dto.titleEn(), dto.bodyEn(),
                dto.referenceId(), dto.read(), dto.createdAt(), s3PresignService.presignGetUrl(merged.imageKey()));
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markRead(Long notificationId, Long userId) {
        Notification notification = EntityLoader.getOrThrow(notificationRepository::findById, notificationId, "알림");
        OwnershipValidator.checkOwner(notification.getUserId(), userId, "알림", "읽음 처리");
        notification.markRead();
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadByUserId(userId);
    }

    @Transactional
    public void deleteById(Long notificationId, Long userId) {
        Notification notification = EntityLoader.getOrThrow(notificationRepository::findById, notificationId, "알림");
        OwnershipValidator.checkOwner(notification.getUserId(), userId, "알림", "삭제");
        notificationRepository.delete(notification);
    }

    @Transactional
    public void deleteAll(Long userId) {
        notificationRepository.deleteByUserId(userId);
    }

    private Set<NotificationType> resolveTypeFilter(String typeGroup) {
        if (typeGroup == null) return null;
        return switch (typeGroup) {
            case "cert"     -> CERT_TYPES;
            case "comment"  -> COMMENT_TYPES;
            case "festival" -> FESTIVAL_TYPES;
            default         -> null;
        };
    }
}
