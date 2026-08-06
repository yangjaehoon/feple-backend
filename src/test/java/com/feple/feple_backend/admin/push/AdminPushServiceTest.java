package com.feple.feple_backend.admin.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.artist.service.ArtistAdminService;
import com.feple.feple_backend.artistfollow.service.ArtistFollowService;
import com.feple.feple_backend.certification.service.FestivalCertificationAdminService;
import com.feple.feple_backend.festival.service.FestivalService;
import com.feple.feple_backend.notification.entity.BroadcastNotification;
import com.feple.feple_backend.notification.repository.BroadcastNotificationRepository;
import com.feple.feple_backend.notification.service.NotificationService;
import com.feple.feple_backend.notification.service.PushNotificationClient;
import com.feple.feple_backend.user.entity.UserDeviceToken;
import com.feple.feple_backend.user.repository.UserDeviceTokenRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class AdminPushServiceTest {

    @Mock UserDeviceTokenRepository deviceTokenRepository;
    @Mock NotificationService notificationService;
    @Mock PushNotificationClient fcmPushService;
    @Mock BroadcastNotificationRepository broadcastNotificationRepository;
    @Mock ArtistFollowService artistFollowService;
    @Mock FestivalCertificationAdminService festivalCertificationAdminService;
    @Mock ArtistAdminService artistService;
    @Mock FestivalService festivalService;

    @InjectMocks AdminPushService service;

    @AfterEach
    void clearTransactionSync() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private UserDeviceToken tokenOf(String token) {
        UserDeviceToken t = mock(UserDeviceToken.class);
        given(t.getToken()).willReturn(token);
        return t;
    }

    // ── getFormData ──────────────────────────────────────────────────────

    @Test
    void getFormData_기기수_이력_아티스트_페스티벌_조합() {
        given(deviceTokenRepository.countDistinctUsers()).willReturn(42L);
        BroadcastNotification history = BroadcastNotification.of("공지", "내용");
        given(broadcastNotificationRepository.findAllByOrderByCreatedAtDesc(any()))
                .willReturn(List.of(history));
        given(artistService.getAllArtistsSortedByName()).willReturn(List.of());
        given(festivalService.getAllFestivals(any())).willReturn(List.of());

        PushFormData result = service.getFormData();

        assertThat(result.deviceCount()).isEqualTo(42L);
        assertThat(result.history()).hasSize(1);
        assertThat(result.history().get(0).title()).isEqualTo("공지");
    }

    // ── sendTest ─────────────────────────────────────────────────────────

    @Test
    void sendTest_정상_발송() {
        UserDeviceToken token = tokenOf("token-a");
        given(deviceTokenRepository.findByUserId(1L)).willReturn(List.of(token));

        service.sendTest(1L, "제목", "내용");

        verify(notificationService).saveAdminBroadcastNotification(1L, "제목", "내용");
        verify(fcmPushService).sendBroadcast(List.of("token-a"), "제목", "내용");
    }

    @Test
    void sendTest_토큰_없으면_예외() {
        given(deviceTokenRepository.findByUserId(1L)).willReturn(List.of());

        assertThatThrownBy(() -> service.sendTest(1L, "제목", "내용"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("등록된 디바이스 토큰이 없습니다");
        verify(notificationService, never()).saveAdminBroadcastNotification(any(), any(), any());
    }

    // ── sendToArtistFollowers ────────────────────────────────────────────

    @Test
    void 아티스트_팔로워_발송_정상() {
        given(artistFollowService.getFollowerUserIds(10L)).willReturn(List.of(1L, 2L));
        given(deviceTokenRepository.findTokensByUserIds(List.of(1L, 2L))).willReturn(List.of("t1", "t2"));

        service.sendToArtistFollowers(10L, "제목", "내용");

        verify(notificationService).saveAdminBroadcastNotifications(List.of(1L, 2L), "제목", "내용");
        verify(fcmPushService).sendBroadcast(List.of("t1", "t2"), "제목", "내용");
    }

    @Test
    void 아티스트_팔로워_없으면_예외() {
        given(artistFollowService.getFollowerUserIds(10L)).willReturn(List.of());

        assertThatThrownBy(() -> service.sendToArtistFollowers(10L, "제목", "내용"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("팔로워가 없습니다");
    }

    @Test
    void 아티스트_팔로워_토큰_전부_비활성이면_예외() {
        given(artistFollowService.getFollowerUserIds(10L)).willReturn(List.of(1L));
        given(deviceTokenRepository.findTokensByUserIds(List.of(1L))).willReturn(List.of());

        assertThatThrownBy(() -> service.sendToArtistFollowers(10L, "제목", "내용"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("발송 대상 기기가 없습니다");
    }

    // ── sendToFestivalCertified ──────────────────────────────────────────

    @Test
    void 페스티벌_인증자_발송_정상() {
        given(festivalCertificationAdminService.getApprovedUserIds(20L)).willReturn(Set.of(3L));
        given(deviceTokenRepository.findTokensByUserIds(List.of(3L))).willReturn(List.of("t3"));

        service.sendToFestivalCertified(20L, "제목", "내용");

        verify(notificationService).saveAdminBroadcastNotifications(List.of(3L), "제목", "내용");
        verify(fcmPushService).sendBroadcast(List.of("t3"), "제목", "내용");
    }

    @Test
    void 페스티벌_인증자_없으면_예외() {
        given(festivalCertificationAdminService.getApprovedUserIds(20L)).willReturn(Set.of());

        assertThatThrownBy(() -> service.sendToFestivalCertified(20L, "제목", "내용"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("인증된 참여자가 없습니다");
    }

    // ── sendToAll ────────────────────────────────────────────────────────

    @Test
    void 전체_발송_정상() {
        given(deviceTokenRepository.findAllTokens()).willReturn(List.of("t1", "t2"));

        service.sendToAll("제목", "내용");

        verify(broadcastNotificationRepository).save(any(BroadcastNotification.class));
        verify(fcmPushService).sendBroadcast(List.of("t1", "t2"), "제목", "내용");
    }

    @Test
    void 전체_발송_토큰_없으면_예외() {
        given(deviceTokenRepository.findAllTokens()).willReturn(List.of());

        assertThatThrownBy(() -> service.sendToAll("제목", "내용"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("등록된 디바이스 토큰이 없습니다");
        verify(broadcastNotificationRepository, never()).save(any());
    }

    // ── 트랜잭션 커밋 이후 발송 ──────────────────────────────────────────

    @Test
    void 활성_트랜잭션_있으면_즉시_발송_안하고_커밋후_발송() {
        given(deviceTokenRepository.findAllTokens()).willReturn(List.of("t1"));
        TransactionSynchronizationManager.initSynchronization();

        service.sendToAll("제목", "내용");

        verify(fcmPushService, never()).sendBroadcast(any(), any(), any());

        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());

        verify(fcmPushService).sendBroadcast(List.of("t1"), "제목", "내용");
    }
}
