package com.feple.feple_backend.notification.service;

import com.feple.feple_backend.global.EntityFinder;
import com.feple.feple_backend.notification.dto.NotificationDto;
import com.feple.feple_backend.notification.entity.Notification;
import com.feple.feple_backend.notification.repository.BroadcastNotificationRepository;
import com.feple.feple_backend.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;
    private final BroadcastNotificationRepository broadcastNotificationRepository;

    public List<NotificationDto> getMyNotifications(Long userId) {
        List<NotificationDto> personal = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationDto::from)
                .toList();
        List<NotificationDto> broadcasts = broadcastNotificationRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(NotificationDto::forBroadcast)
                .toList();
        return Stream.concat(personal.stream(), broadcasts.stream())
                .sorted(Comparator.comparing(NotificationDto::createdAt).reversed())
                .toList();
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markRead(Long notificationId, Long userId) {
        Notification n = EntityFinder.getOrThrow(notificationRepository::findById, notificationId, "알림");
        if (!n.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 알림만 읽음 처리할 수 있습니다.");
        }
        n.markRead();
        notificationRepository.save(n);
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadByUserId(userId);
    }
}
