package com.feple.feple_backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.feple.feple_backend.file.service.S3PresignService;
import com.feple.feple_backend.notification.dto.NotificationDto;
import com.feple.feple_backend.notification.entity.BroadcastNotification;
import com.feple.feple_backend.notification.entity.Notification;
import com.feple.feple_backend.notification.entity.NotificationType;
import com.feple.feple_backend.notification.repository.BroadcastNotificationRepository;
import com.feple.feple_backend.notification.repository.NotificationRepository;
import com.feple.feple_backend.notification.service.NotificationQueryService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class NotificationQueryServiceTest {

    @Mock NotificationRepository notificationRepository;
    @Mock BroadcastNotificationRepository broadcastNotificationRepository;
    @Mock S3PresignService s3PresignService;

    @InjectMocks NotificationQueryService notificationQueryService;

    private Notification mockNotification(Long id, String title, String body,
                                           String titleEn, String bodyEn, LocalDateTime createdAt) {
        return mockNotification(id, NotificationType.NEW_FESTIVAL, title, body, titleEn, bodyEn, createdAt);
    }

    private Notification mockNotification(Long id, NotificationType type, String title, String body,
                                           String titleEn, String bodyEn, LocalDateTime createdAt) {
        Notification n = mock(Notification.class);
        given(n.getId()).willReturn(id);
        given(n.getType()).willReturn(type);
        given(n.getTitle()).willReturn(title);
        given(n.getBody()).willReturn(body);
        given(n.getTitleEn()).willReturn(titleEn);
        given(n.getBodyEn()).willReturn(bodyEn);
        given(n.getReferenceId()).willReturn(null);
        given(n.isRead()).willReturn(false);
        given(n.getCreatedAt()).willReturn(createdAt);
        return n;
    }

    @Test
    void getMyNotifications_개인_방송_병합_최신순_정렬() {
        LocalDateTime newer = LocalDateTime.of(2026, 1, 1, 12, 0);
        LocalDateTime older = LocalDateTime.of(2026, 1, 1, 11, 0);

        Notification n = mockNotification(1L, "제목", "내용", "Title", "Body", newer);

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

        Notification n1 = mockNotification(1L, "제목1", "내용1", null, null, t1);
        Notification n2 = mockNotification(2L, "제목2", "내용2", null, null, t2);

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
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("본인의 알림만 읽음 처리");
    }

    @Test
    void markAllRead_호출() {
        notificationQueryService.markAllRead(1L);

        then(notificationRepository).should().markAllReadByUserId(1L);
    }

    // ── getMyNotifications: typeGroup 필터 ────────────────────────────

    @Test
    void getMyNotifications_cert_타입그룹이면_인증관련_알림만_포함되고_공지는_제외() {
        LocalDateTime t1 = LocalDateTime.of(2026, 1, 1, 12, 0);
        Notification certNotification = mockNotification(1L, NotificationType.CERT_APPROVED, "인증승인", "내용", null, null, t1);
        Notification commentNotification = mockNotification(2L, NotificationType.NEW_COMMENT, "댓글", "내용", null, null, t1);

        given(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(PageRequest.class)))
                .willReturn(List.of(certNotification, commentNotification));

        Page<NotificationDto> result = notificationQueryService.getMyNotifications(1L, PageRequest.of(0, 10), "cert");

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).type()).isEqualTo(NotificationType.CERT_APPROVED);
        then(broadcastNotificationRepository).should(org.mockito.Mockito.never()).findAllByOrderByCreatedAtDesc(any());
    }

    @Test
    void getMyNotifications_알수없는_타입그룹이면_필터없이_전체_조회() {
        LocalDateTime t1 = LocalDateTime.of(2026, 1, 1, 12, 0);
        Notification n = mockNotification(1L, NotificationType.NEW_COMMENT, "댓글", "내용", null, null, t1);

        given(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(PageRequest.class)))
                .willReturn(List.of(n));
        given(broadcastNotificationRepository.findAllByOrderByCreatedAtDesc(any(PageRequest.class)))
                .willReturn(List.of());

        Page<NotificationDto> result = notificationQueryService.getMyNotifications(1L, PageRequest.of(0, 10), "unknown");

        assertThat(result.getContent()).hasSize(1);
    }

    // ── resolveImageUrl ───────────────────────────────────────────────

    @Test
    void getMyNotifications_이미지키_있으면_presign_URL로_변환() {
        LocalDateTime t1 = LocalDateTime.of(2026, 1, 1, 12, 0);
        Notification n = mockNotification(1L, "제목", "내용", null, null, t1);
        given(n.getImageKey()).willReturn("festivals/poster.jpg");

        given(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(PageRequest.class)))
                .willReturn(List.of(n));
        given(broadcastNotificationRepository.findAllByOrderByCreatedAtDesc(any(PageRequest.class)))
                .willReturn(List.of());
        given(s3PresignService.presignGetUrl("festivals/poster.jpg")).willReturn("https://cdn.example.com/festivals/poster.jpg");

        Page<NotificationDto> result = notificationQueryService.getMyNotifications(1L, PageRequest.of(0, 10), null);

        assertThat(result.getContent().get(0).imageUrl()).isEqualTo("https://cdn.example.com/festivals/poster.jpg");
    }

    // ── deleteById ────────────────────────────────────────────────────

    @Test
    void deleteById_성공() {
        given(notificationRepository.deleteByIdAndUserId(10L, 1L)).willReturn(1);

        notificationQueryService.deleteById(10L, 1L);

        then(notificationRepository).should().deleteByIdAndUserId(10L, 1L);
    }

    @Test
    void deleteById_대상없으면_예외() {
        given(notificationRepository.deleteByIdAndUserId(99L, 1L)).willReturn(0);

        assertThatThrownBy(() -> notificationQueryService.deleteById(99L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 알림을 찾을 수 없습니다.");
    }

    // ── deleteAll / removeAllByPostIds / removeAllByFestivalId ────────

    @Test
    void deleteAll_호출() {
        notificationQueryService.deleteAll(1L);

        then(notificationRepository).should().deleteByUserId(1L);
    }

    @Test
    void removeAllByPostIds_호출() {
        notificationQueryService.removeAllByPostIds(List.of(1L, 2L));

        then(notificationRepository).should().deleteByPostIdIn(List.of(1L, 2L));
    }

    @Test
    void removeAllByFestivalId_호출() {
        notificationQueryService.removeAllByFestivalId(5L);

        then(notificationRepository).should().deleteByFestivalId(5L);
    }
}
