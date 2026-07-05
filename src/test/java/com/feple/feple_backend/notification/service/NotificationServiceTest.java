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
import com.feple.feple_backend.notification.entity.NotificationPreference;
import com.feple.feple_backend.notification.entity.NotificationType;
import com.feple.feple_backend.notification.repository.NotificationRepository;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.event.PostDeletedByAdminEvent;
import com.feple.feple_backend.post.event.PostLikedEvent;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserDeviceTokenRepository;
import com.feple.feple_backend.user.repository.UserDeviceTokenRepository.TokenLanguageProjection;
import com.feple.feple_backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository notificationRepository;
    @Mock ArtistFollowRepository artistFollowRepository;
    @Mock ArtistRepository artistRepository;
    @Mock UserDeviceTokenRepository deviceTokenRepository;
    @Mock UserRepository userRepository;
    @Mock FestivalRepository festivalRepository;
    @Mock PostRepository postRepository;
    @Mock PushNotificationClient fcmPushService;
    @Mock NotificationPreferenceService preferenceService;

    @InjectMocks NotificationService service;

    private User user(Long id) {
        return User.builder().id(id).oauthId("o" + id).nickname("유저" + id).build();
    }

    private NotificationPreference enabledPreference() {
        return NotificationPreference.defaultFor(1L);
    }

    private TokenLanguageProjection token(String value, String lang) {
        TokenLanguageProjection t = mock(TokenLanguageProjection.class);
        lenient().when(t.getToken()).thenReturn(value);
        lenient().when(t.getLanguage()).thenReturn(lang);
        return t;
    }

    @BeforeEach
    void setUpDefaults() {
        lenient().when(deviceTokenRepository.findTokensWithLanguageByUserIds(anyList())).thenReturn(List.of());
    }

    // ── onArtistAddedToFestival ───────────────────────────────────────────

    @Test
    void 아티스트_페스티벌추가_팔로워_없으면_알림_없음() {
        given(artistFollowRepository.findByArtistId(1L)).willReturn(List.of());

        service.onArtistAddedToFestival(new ArtistAddedToFestivalEvent(1L, "아이유", "IU", 10L, "펜타포트", "Pentaport"));

        then(notificationRepository).shouldHaveNoInteractions();
    }

    @Test
    void 아티스트_페스티벌추가_페스티벌_없으면_알림_없음() {
        ArtistFollow follow = mock(ArtistFollow.class);
        given(artistFollowRepository.findByArtistId(1L)).willReturn(List.of(follow));
        given(festivalRepository.findById(10L)).willReturn(Optional.empty());

        service.onArtistAddedToFestival(new ArtistAddedToFestivalEvent(1L, "아이유", "IU", 10L, "펜타포트", "Pentaport"));

        then(notificationRepository).shouldHaveNoInteractions();
    }

    @Test
    void 아티스트_페스티벌추가_성공시_저장_및_푸시() {
        ArtistFollow follow = mock(ArtistFollow.class);
        given(follow.getUserId()).willReturn(100L);
        given(artistFollowRepository.findByArtistId(1L)).willReturn(List.of(follow));
        Festival festival = Festival.builder().id(10L).title("펜타포트").build();
        given(festivalRepository.findById(10L)).willReturn(Optional.of(festival));
        User user = user(100L);
        given(userRepository.findAllById(List.of(100L))).willReturn(List.of(user));
        NotificationPreference pref = enabledPreference();
        given(preferenceService.getOrCreateBatch(List.of(100L))).willReturn(Map.of(100L, pref));
        TokenLanguageProjection koToken = token("tok1", "ko");
        given(deviceTokenRepository.findTokensWithLanguageByUserIds(List.of(100L)))
                .willReturn(List.of(koToken));

        service.onArtistAddedToFestival(new ArtistAddedToFestivalEvent(1L, "아이유", "IU", 10L, "펜타포트", "Pentaport"));

        then(notificationRepository).should().saveAll(anyList());
        then(fcmPushService).should().sendMulticast(eq(List.of("tok1")), any(), any(), eq("10"), eq(NotificationType.NEW_FESTIVAL));
    }

    // ── onCertificationApproved / onCertificationRejected ────────────────

    @Test
    void 인증승인_유저_없으면_무시() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        service.onCertificationApproved(new CertificationApprovedEvent(1L, "펜타포트", "Pentaport", 10L));

        then(notificationRepository).shouldHaveNoInteractions();
    }

    @Test
    void 인증승인_성공() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(festivalRepository.findById(10L)).willReturn(Optional.of(Festival.builder().id(10L).title("펜타포트").build()));
        given(preferenceService.getOrCreate(1L)).willReturn(enabledPreference());

        service.onCertificationApproved(new CertificationApprovedEvent(1L, "펜타포트", "Pentaport", 10L));

        then(notificationRepository).should().save(any());
    }

    @Test
    void 인증거절_성공() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(festivalRepository.findById(10L)).willReturn(Optional.of(Festival.builder().id(10L).title("펜타포트").build()));
        given(preferenceService.getOrCreate(1L)).willReturn(enabledPreference());

        service.onCertificationRejected(new CertificationRejectedEvent(1L, "펜타포트", "Pentaport", 10L, "사진 불명확"));

        then(notificationRepository).should().save(any());
    }

    // ── onSongRequestApproved / onSongRequestRejected ────────────────────

    @Test
    void 노래요청_승인_유저_없으면_무시() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        service.onSongRequestApproved(new SongRequestApprovedEvent(1L, 5L, "좋은날", "아이유", "IU"));

        then(notificationRepository).shouldHaveNoInteractions();
    }

    @Test
    void 노래요청_승인_성공() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(artistRepository.findById(5L)).willReturn(Optional.of(Artist.builder().id(5L).name("아이유").build()));
        given(preferenceService.getOrCreate(1L)).willReturn(enabledPreference());

        service.onSongRequestApproved(new SongRequestApprovedEvent(1L, 5L, "좋은날", "아이유", "IU"));

        then(notificationRepository).should().save(any());
    }

    @Test
    void 노래요청_거절_성공() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(preferenceService.getOrCreate(1L)).willReturn(enabledPreference());

        service.onSongRequestRejected(new SongRequestRejectedEvent(1L, 5L, "좋은날", "아이유", "중복 요청"));

        then(notificationRepository).should().save(any());
    }

    // ── onArtistSuggestionProcessed ───────────────────────────────────────

    @Test
    void 아티스트제안_처리_유저_없으면_무시() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        service.onArtistSuggestionProcessed(new ArtistSuggestionProcessedEvent(1L, 5L, "새아티스트", "등록완료"));

        then(notificationRepository).shouldHaveNoInteractions();
    }

    @Test
    void 아티스트제안_처리_아티스트_있으면_아티스트연결_알림() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(artistRepository.findById(5L)).willReturn(Optional.of(Artist.builder().id(5L).name("새아티스트").nameEn("NewArtist").build()));
        given(preferenceService.getOrCreate(1L)).willReturn(enabledPreference());

        service.onArtistSuggestionProcessed(new ArtistSuggestionProcessedEvent(1L, 5L, "새아티스트", "등록완료"));

        then(notificationRepository).should().save(any());
    }

    @Test
    void 아티스트제안_처리_아티스트_없으면_festival_null로_알림() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(preferenceService.getOrCreate(1L)).willReturn(enabledPreference());

        service.onArtistSuggestionProcessed(new ArtistSuggestionProcessedEvent(1L, null, "미등록아티스트", "반려"));

        then(notificationRepository).should().save(any());
        then(artistRepository).should(never()).findById(any());
    }

    // ── onCommentCreated ──────────────────────────────────────────────────

    @Test
    void 댓글생성_게시글작성자와_원댓글작성자_모두_있으면_둘다_알림() {
        given(userRepository.findById(100L)).willReturn(Optional.of(user(100L)));
        given(userRepository.findById(200L)).willReturn(Optional.of(user(200L)));
        given(preferenceService.getOrCreate(any())).willReturn(enabledPreference());

        service.onCommentCreated(new CommentCreatedEvent(100L, "댓글러", "제목", 1L, 200L, 999L));

        then(notificationRepository).should(org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void 댓글생성_대상_전부_null이면_알림_없음() {
        service.onCommentCreated(new CommentCreatedEvent(null, "댓글러", "제목", 1L, null, 999L));

        then(notificationRepository).shouldHaveNoInteractions();
    }

    // ── onPostLiked ───────────────────────────────────────────────────────

    @Test
    void 게시글좋아요_작성자_없으면_무시() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        service.onPostLiked(new PostLikedEvent(1L, "좋아요러", "제목", 5L));

        then(notificationRepository).shouldHaveNoInteractions();
    }

    @Test
    void 게시글좋아요_성공() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(preferenceService.getOrCreate(1L)).willReturn(enabledPreference());

        service.onPostLiked(new PostLikedEvent(1L, "좋아요러", "제목", 5L));

        then(notificationRepository).should().save(any());
    }

    // ── onPostDeletedByAdmin ──────────────────────────────────────────────

    @Test
    void 관리자삭제_성공() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(preferenceService.getOrCreate(1L)).willReturn(enabledPreference());

        service.onPostDeletedByAdmin(new PostDeletedByAdminEvent(1L, "제목"));

        then(notificationRepository).should().save(any());
    }

    // ── sendFestivalReminders ─────────────────────────────────────────────

    @Test
    void 페스티벌리마인더_유저없으면_무시() {
        service.sendFestivalReminders(10L, "펜타포트", "Pentaport", List.of(), 7);

        then(notificationRepository).shouldHaveNoInteractions();
    }

    @Test
    void 페스티벌리마인더_성공() {
        given(userRepository.findAllById(List.of(100L))).willReturn(List.of(user(100L)));
        given(preferenceService.getOrCreateBatch(List.of(100L))).willReturn(Map.of(100L, enabledPreference()));

        service.sendFestivalReminders(10L, "펜타포트", "Pentaport", List.of(100L), 7);

        then(notificationRepository).should().saveAll(anyList());
    }

    // ── 알림 설정 꺼져있으면 푸시 안 감 ────────────────────────────────────

    @Test
    void 알림설정_비활성화면_저장은_되지만_푸시는_안함() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        NotificationPreference disabled = mock(NotificationPreference.class);
        given(disabled.isEnabledFor(any())).willReturn(false);
        given(preferenceService.getOrCreate(1L)).willReturn(disabled);

        service.onPostLiked(new PostLikedEvent(1L, "좋아요러", "제목", 5L));

        then(notificationRepository).should().save(any());
        then(deviceTokenRepository).should(never()).findTokensWithLanguageByUserIds(any());
    }

    // ── 언어별 토큰 분기 발송 ─────────────────────────────────────────────

    @Test
    void 언어별_토큰이_각각_다른_메시지로_발송됨() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(preferenceService.getOrCreate(1L)).willReturn(enabledPreference());
        TokenLanguageProjection koToken = token("ko-tok", "ko");
        TokenLanguageProjection enToken = token("en-tok", "en");
        given(deviceTokenRepository.findTokensWithLanguageByUserIds(List.of(1L)))
                .willReturn(List.of(koToken, enToken));

        service.onPostLiked(new PostLikedEvent(1L, "좋아요러", "제목", 5L));

        then(fcmPushService).should().sendMulticast(eq(List.of("ko-tok")), any(), any(), any(), eq(NotificationType.POST_LIKED));
        then(fcmPushService).should().sendMulticast(eq(List.of("en-tok")), any(), any(), any(), eq(NotificationType.POST_LIKED));
    }
}
