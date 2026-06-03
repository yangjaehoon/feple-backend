package com.feple.feple_backend.notification.controller;

import com.feple.feple_backend.notification.dto.NotificationPreferenceDto;
import com.feple.feple_backend.notification.dto.UpdateNotificationPreferenceDto;
import com.feple.feple_backend.notification.service.NotificationPreferenceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "알림 설정", description = "푸시 알림 수신 항목 설정")
@RestController
@RequestMapping("/users/me/notification-preferences")
@RequiredArgsConstructor
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;

    @GetMapping
    public ResponseEntity<NotificationPreferenceDto> getPreferences(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(preferenceService.getPreferences(userId));
    }

    @PutMapping
    public ResponseEntity<NotificationPreferenceDto> updatePreferences(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateNotificationPreferenceDto dto) {
        return ResponseEntity.ok(preferenceService.updatePreferences(userId, dto));
    }
}
