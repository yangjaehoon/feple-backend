package com.feple.feple_backend.notification.service;

import com.feple.feple_backend.file.service.S3PresignService;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.notification.dto.NotificationDto;
import com.feple.feple_backend.notification.entity.Notification;
import com.feple.feple_backend.notification.entity.NotificationType;
import com.feple.feple_backend.notification.repository.BroadcastNotificationRepository;
import com.feple.feple_backend.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

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
            NotificationType.ARTIST_SUGGESTION_PROCESSED);

    private final NotificationRepository notificationRepository;
    private final BroadcastNotificationRepository broadcastNotificationRepository;
    private final S3PresignService s3PresignService;

    public Page<NotificationDto> getMyNotifications(Long userId, Pageable pageable, String typeGroup) {
        Set<NotificationType> typeFilter = resolveTypeFilter(typeGroup);

        List<NotificationDto> personal = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, MAX_PERSONAL))
                .stream()
                .map(n -> NotificationDto.from(n, resolveImageUrl(n)))
                .toList();

        Stream<NotificationDto> mergedStream = personal.stream();

        // 타입 필터가 없을 때만 공지 포함
        if (typeFilter == null) {
            List<NotificationDto> broadcasts = broadcastNotificationRepository
                    .findAllByOrderByCreatedAtDesc(PageRequest.of(0, MAX_BROADCAST))
                    .stream()
                    .map(NotificationDto::forBroadcast)
                    .toList();
            mergedStream = Stream.concat(mergedStream, broadcasts.stream());
        }

        List<NotificationDto> all = mergedStream
                .filter(n -> typeFilter == null || typeFilter.contains(n.type()))
                .sorted(Comparator.comparing(NotificationDto::createdAt).reversed())
                .toList();

        int total = all.size();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), total);
        List<NotificationDto> paged = start >= total ? List.of() : all.subList(start, end);
        return new PageImpl<>(paged, pageable, total);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markRead(Long notificationId, Long userId) {
        Notification notification = EntityLoader.getOrThrow(notificationRepository::findById, notificationId, "알림");
        if (!notification.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 알림만 읽음 처리할 수 있습니다.");
        }
        notification.markRead();
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadByUserId(userId);
    }

    @Transactional
    public void deleteById(Long notificationId, Long userId) {
        int deleted = notificationRepository.deleteByIdAndUserId(notificationId, userId);
        if (deleted == 0) {
            throw new IllegalArgumentException("해당 알림을 찾을 수 없습니다.");
        }
    }

    @Transactional
    public void deleteAll(Long userId) {
        notificationRepository.deleteByUserId(userId);
    }

    private String resolveImageUrl(Notification n) {
        String key = n.getImageKey();
        return key != null ? s3PresignService.presignGetUrl(key) : null;
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
