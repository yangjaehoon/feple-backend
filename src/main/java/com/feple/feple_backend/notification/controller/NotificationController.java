package com.feple.feple_backend.notification.controller;

import com.feple.feple_backend.notification.dto.NotificationDto;
import com.feple.feple_backend.notification.service.NotificationQueryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "알림", description = "알림 목록 조회·읽음 처리")
@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationQueryService notificationQueryService;

    /** 내 알림 목록 (페이지네이션) */
    @GetMapping("/my")
    public ResponseEntity<Page<NotificationDto>> getMyNotifications(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationQueryService.getMyNotifications(userId, PageRequest.of(page, size)));
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
