package com.feple.feple_backend.notification.controller;

import com.feple.feple_backend.notification.dto.NotificationDto;
import com.feple.feple_backend.notification.service.NotificationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationQueryService notificationQueryService;

    /** 내 알림 목록 */
    @GetMapping("/my")
    public ResponseEntity<List<NotificationDto>> getMyNotifications(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(notificationQueryService.getMyNotifications(userId));
    }

    /** 읽지 않은 알림 수 */
    @GetMapping("/my/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(Map.of("count", notificationQueryService.getUnreadCount(userId)));
    }

    /** 단건 읽음 처리 */
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        notificationQueryService.markRead(id, userId);
        return ResponseEntity.noContent().build();
    }

    /** 전체 읽음 처리 */
    @PatchMapping("/my/read-all")
    public ResponseEntity<Void> markAllRead(
            @AuthenticationPrincipal Long userId) {
        notificationQueryService.markAllRead(userId);
        return ResponseEntity.noContent().build();
    }
}
