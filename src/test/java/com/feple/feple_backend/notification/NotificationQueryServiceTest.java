package com.feple.feple_backend.notification;

import com.feple.feple_backend.notification.dto.NotificationDto;
import com.feple.feple_backend.notification.entity.BroadcastNotification;
import com.feple.feple_backend.notification.entity.Notification;
import com.feple.feple_backend.notification.entity.NotificationType;
import com.feple.feple_backend.notification.repository.BroadcastNotificationRepository;
import com.feple.feple_backend.notification.repository.NotificationRepository;
import com.feple.feple_backend.notification.service.NotificationQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class NotificationQueryServiceTest {

    @Mock NotificationRepository notificationRepository;
    @Mock BroadcastNotificationRepository broadcastNotificationRepository;

    @InjectMocks NotificationQueryService notificationQueryService;

    @Test
    void getMyNotifications_개인_방송_병합_최신순_정렬() {
        LocalDateTime newer = LocalDateTime.of(2026, 1, 1, 12, 0);
        LocalDateTime older = LocalDateTime.of(2026, 1, 1, 11, 0);

        Notification n = mock(Notification.class);
        given(n.getId()).willReturn(1L);
        given(n.getType()).willReturn(NotificationType.NEW_FESTIVAL);
        given(n.getTitle()).willReturn("제목");
        given(n.getBody()).willReturn("내용");
        given(n.getTitleEn()).willReturn("Title");
        given(n.getBodyEn()).willReturn("Body");
        given(n.getReferenceId()).willReturn(null);
        given(n.isRead()).willReturn(false);
        given(n.getCreatedAt()).willReturn(newer);

        BroadcastNotification b = mock(BroadcastNotification.class);
        given(b.getId()).willReturn(10L);
        given(b.getTitle()).willReturn("공지");
        given(b.getBody()).willReturn("공지 내용");
        given(b.getCreatedAt()).willReturn(older);

        given(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(PageRequest.class)))
                .willReturn(List.of(n));
        given(broadcastNotificationRepository.findAllByOrderByCreatedAtDesc(any(PageRequest.class)))
                .willReturn(List.of(b));

        Pageable pageable = PageRequest.of(0, 10);
        Page<NotificationDto> result = notificationQueryService.getMyNotifications(1L, pageable, null);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).id()).isEqualTo(1L);
        assertThat(result.getContent().get(0).createdAt()).isEqualTo(newer);
    }

    @Test
    void getMyNotifications_페이지_범위_초과_빈_리스트() {
        LocalDateTime t1 = LocalDateTime.of(2026, 1, 1, 12, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 1, 1, 11, 0);

        Notification n1 = mock(Notification.class);
        given(n1.getId()).willReturn(1L);
        given(n1.getType()).willReturn(NotificationType.NEW_FESTIVAL);
        given(n1.getTitle()).willReturn("제목1");
        given(n1.getBody()).willReturn("내용1");
        given(n1.getTitleEn()).willReturn(null);
        given(n1.getBodyEn()).willReturn(null);
        given(n1.getReferenceId()).willReturn(null);
        given(n1.isRead()).willReturn(false);
        given(n1.getCreatedAt()).willReturn(t1);

        Notification n2 = mock(Notification.class);
        given(n2.getId()).willReturn(2L);
        given(n2.getType()).willReturn(NotificationType.NEW_FESTIVAL);
        given(n2.getTitle()).willReturn("제목2");
        given(n2.getBody()).willReturn("내용2");
        given(n2.getTitleEn()).willReturn(null);
        given(n2.getBodyEn()).willReturn(null);
        given(n2.getReferenceId()).willReturn(null);
        given(n2.isRead()).willReturn(false);
        given(n2.getCreatedAt()).willReturn(t2);

        given(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(PageRequest.class)))
                .willReturn(List.of(n1, n2));
        given(broadcastNotificationRepository.findAllByOrderByCreatedAtDesc(any(PageRequest.class)))
                .willReturn(List.of());

        // 2개 존재, page=1 size=10 → offset=10 > 2
        Pageable pageable = PageRequest.of(1, 10);
        Page<NotificationDto> result = notificationQueryService.getMyNotifications(1L, pageable, null);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void getUnreadCount_호출() {
        given(notificationRepository.countByUserIdAndIsReadFalse(1L)).willReturn(3L);

        long result = notificationQueryService.getUnreadCount(1L);

        assertThat(result).isEqualTo(3L);
    }

    @Test
    void markRead_성공() {
        Notification n = mock(Notification.class);
        given(n.getUserId()).willReturn(1L);
        given(notificationRepository.findById(10L)).willReturn(Optional.of(n));

        notificationQueryService.markRead(10L, 1L);

        then(n).should().markRead();
    }

    @Test
    void markRead_다른_사용자_예외() {
        Notification n = mock(Notification.class);
        given(n.getUserId()).willReturn(99L);
        given(notificationRepository.findById(10L)).willReturn(Optional.of(n));

        assertThatThrownBy(() -> notificationQueryService.markRead(10L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("본인의 알림만 읽음 처리");
    }

    @Test
    void markAllRead_호출() {
        notificationQueryService.markAllRead(1L);

        then(notificationRepository).should().markAllReadByUserId(1L);
    }
}
