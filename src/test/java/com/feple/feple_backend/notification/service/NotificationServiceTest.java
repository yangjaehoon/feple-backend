package com.feple.feple_backend.notification.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

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
import com.feple.feple_backend.festival.suggestion.event.FestivalSuggestionProcessedEvent;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.global.KoreaClock;
import com.feple.feple_backend.notification.entity.NotificationPreference;
import com.feple.feple_backend.notification.entity.NotificationType;
import com.feple.feple_backend.notification.entity.PendingPush;
import com.feple.feple_backend.notification.repository.NotificationRepository;
import com.feple.feple_backend.notification.repository.PendingPushRepository;
import com.feple.feple_backend.post.event.PostDeletedByAdminEvent;
import com.feple.feple_backend.post.event.PostLikedEvent;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.event.AdminPointGrantedEvent;
import com.feple.feple_backend.user.repository.UserDeviceTokenRepository;
import com.feple.feple_backend.user.repository.UserDeviceTokenRepository.TokenLanguageProjection;
import com.feple.feple_backend.user.repository.UserRepository;
import com.feple.feple_backend.userblock.service.UserBlockService;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    @Mock UserBlockService userBlockService;
    @Mock FileStorageService fileStorageService;
    @Mock PendingPushRepository pendingPushRepository;
    /** 실제 벽시계에 의존하면 새벽 지연 발송 로직 때문에 테스트가 시간대별로 깨진다 —
     * 기본을 낮 시간으로 고정하고, 새벽 시나리오 테스트에서만 개별 재stub한다. */
    @Mock KoreaClock koreaClock;

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
        lenient().when(koreaClock.now()).thenReturn(LocalTime.of(14, 0));
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
        then(fcmPushService).should().sendMulticast(eq(List.of("tok1")),
                argThat(m -> "10".equals(m.resourceId()) && m.type() == NotificationType.NEW_FESTIVAL));
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
    void 인증승인_페스티벌에_포스터_있으면_푸시에_이미지_URL_포함() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(festivalRepository.findById(10L)).willReturn(Optional.of(
                Festival.builder().id(10L).title("펜타포트").posterKey("festival-posters/2026/p.jpg").build()));
        given(preferenceService.getOrCreate(1L)).willReturn(enabledPreference());
        given(fileStorageService.buildUrl("festival-posters/2026/p.jpg"))
                .willReturn("https://cdn.feple.com/festival-posters/2026/p.jpg");
        TokenLanguageProjection koToken = token("tok1", "ko");
        given(deviceTokenRepository.findTokensWithLanguageByUserIds(List.of(1L)))
                .willReturn(List.of(koToken));

        service.onCertificationApproved(new CertificationApprovedEvent(1L, "펜타포트", "Pentaport", 10L));

        then(fcmPushService).should().sendMulticast(anyList(),
                argThat(m -> "https://cdn.feple.com/festival-posters/2026/p.jpg".equals(m.imageUrl())));
    }

    @Test
    void 인증거절_성공() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(festivalRepository.findById(10L)).willReturn(Optional.of(Festival.builder().id(10L).title("펜타포트").build()));
        given(preferenceService.getOrCreate(1L)).willReturn(enabledPreference());

        service.onCertificationRejected(new CertificationRejectedEvent(1L, "펜타포트", "Pentaport", 10L, "사진 불명확"));

        then(notificationRepository).should().save(any());
    }

    // ── onAdminPointGranted ────────────────────────────────────────────

    @Test
    void 관리자_포인트_지급_유저_없으면_무시() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        service.onAdminPointGranted(new AdminPointGrantedEvent(1L, 100, "이벤트 보상"));

        then(notificationRepository).shouldHaveNoInteractions();
    }

    @Test
    void 관리자_포인트_지급_성공() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(preferenceService.getOrCreate(1L)).willReturn(enabledPreference());

        service.onAdminPointGranted(new AdminPointGrantedEvent(1L, 100, "이벤트 보상"));

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

    // ── onFestivalSuggestionProcessed ─────────────────────────────────────

    @Test
    void 페스티벌제안_처리_유저_없으면_무시() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        service.onFestivalSuggestionProcessed(new FestivalSuggestionProcessedEvent(1L, 10L, "새페스티벌", "등록완료"));

        then(notificationRepository).shouldHaveNoInteractions();
    }

    @Test
    void 페스티벌제안_처리_페스티벌_있으면_페스티벌연결_알림() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(festivalRepository.findById(10L)).willReturn(Optional.of(Festival.builder().id(10L).title("새페스티벌").titleEn("NewFestival").build()));
        given(preferenceService.getOrCreate(1L)).willReturn(enabledPreference());

        service.onFestivalSuggestionProcessed(new FestivalSuggestionProcessedEvent(1L, 10L, "새페스티벌", "등록완료"));

        then(notificationRepository).should().save(any());
    }

    @Test
    void 페스티벌제안_처리_페스티벌_없으면_artist_null로_알림() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(preferenceService.getOrCreate(1L)).willReturn(enabledPreference());

        service.onFestivalSuggestionProcessed(new FestivalSuggestionProcessedEvent(1L, null, "미등록페스티벌", "반려"));

        then(notificationRepository).should().save(any());
        then(festivalRepository).should(never()).findById(any());
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

    @Test
    void 댓글생성_게시글작성자가_댓글러를_차단했으면_알림_없음() {
        given(userBlockService.isBlocked(100L, 999L)).willReturn(true);

        service.onCommentCreated(new CommentCreatedEvent(100L, "댓글러", "제목", 1L, null, 999L));

        then(notificationRepository).shouldHaveNoInteractions();
    }

    @Test
    void 댓글생성_원댓글작성자가_댓글러를_차단했으면_대댓글알림_없음() {
        given(userBlockService.isBlocked(200L, 999L)).willReturn(true);

        service.onCommentCreated(new CommentCreatedEvent(null, "댓글러", "제목", 1L, 200L, 999L));

        then(notificationRepository).shouldHaveNoInteractions();
    }

    @Test
    void 댓글생성_게시글작성자_조회안되면_무시() {
        given(userBlockService.isBlocked(100L, 999L)).willReturn(false);
        given(userRepository.findById(100L)).willReturn(Optional.empty());

        service.onCommentCreated(new CommentCreatedEvent(100L, "댓글러", "제목", 1L, null, 999L));

        then(notificationRepository).shouldHaveNoInteractions();
    }

    @Test
    void 댓글생성_원댓글작성자_조회안되면_무시() {
        given(userBlockService.isBlocked(200L, 999L)).willReturn(false);
        given(userRepository.findById(200L)).willReturn(Optional.empty());

        service.onCommentCreated(new CommentCreatedEvent(null, "댓글러", "제목", 1L, 200L, 999L));

        then(notificationRepository).shouldHaveNoInteractions();
    }

    // ── onPostLiked ───────────────────────────────────────────────────────

    @Test
    void 게시글좋아요_작성자가_좋아요누른사람을_차단했으면_무시() {
        given(userBlockService.isBlocked(1L, 99L)).willReturn(true);

        service.onPostLiked(new PostLikedEvent(1L, "좋아요러", "제목", 5L, 99L));

        then(notificationRepository).shouldHaveNoInteractions();
        then(userRepository).should(never()).findById(any());
    }

    @Test
    void 게시글좋아요_작성자_없으면_무시() {
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        service.onPostLiked(new PostLikedEvent(1L, "좋아요러", "제목", 5L, 99L));

        then(notificationRepository).shouldHaveNoInteractions();
    }

    @Test
    void 게시글좋아요_성공() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(preferenceService.getOrCreate(1L)).willReturn(enabledPreference());

        service.onPostLiked(new PostLikedEvent(1L, "좋아요러", "제목", 5L, 99L));

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

        service.onPostLiked(new PostLikedEvent(1L, "좋아요러", "제목", 5L, 99L));

        then(notificationRepository).should().save(any());
        then(deviceTokenRepository).should(never()).findTokensWithLanguageByUserIds(any());
    }

    // ── 심야시간(00:00~07:00) 알림 차단 ──────────────────────────────────

    @Test
    void 심야시간_설정켜져있고_새벽3시면_저장은_되지만_푸시는_안함() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        NotificationPreference quietHours = mock(NotificationPreference.class);
        given(quietHours.isEnabledFor(any())).willReturn(true);
        given(quietHours.isQuietHoursEnabled()).willReturn(true);
        given(preferenceService.getOrCreate(1L)).willReturn(quietHours);
        lenient().when(koreaClock.now()).thenReturn(LocalTime.of(3, 0));

        service.onPostLiked(new PostLikedEvent(1L, "좋아요러", "제목", 5L, 99L));

        then(notificationRepository).should().save(any());
        then(deviceTokenRepository).should(never()).findTokensWithLanguageByUserIds(any());
    }

    @Test
    void 심야시간_설정켜져있어도_낮시간이면_정상_푸시() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        NotificationPreference quietHours = mock(NotificationPreference.class);
        given(quietHours.isEnabledFor(any())).willReturn(true);
        given(quietHours.isQuietHoursEnabled()).willReturn(true);
        given(preferenceService.getOrCreate(1L)).willReturn(quietHours);

        service.onPostLiked(new PostLikedEvent(1L, "좋아요러", "제목", 5L, 99L));

        then(deviceTokenRepository).should().findTokensWithLanguageByUserIds(List.of(1L));
    }

    @Test
    void 심야시간_설정꺼져있으면_새벽에도_정상_푸시() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(preferenceService.getOrCreate(1L)).willReturn(enabledPreference());
        lenient().when(koreaClock.now()).thenReturn(LocalTime.of(3, 0));

        service.onPostLiked(new PostLikedEvent(1L, "좋아요러", "제목", 5L, 99L));

        then(deviceTokenRepository).should().findTokensWithLanguageByUserIds(List.of(1L));
    }

    // ── 새벽 자동 알림 지연 발송(댓글/좋아요 제외) ──────────────────────────

    @Test
    void 새벽시간_인증승인알림은_즉시발송아닌_대기열적재() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(festivalRepository.findById(10L)).willReturn(Optional.of(Festival.builder().id(10L).title("펜타포트").build()));
        given(preferenceService.getOrCreate(1L)).willReturn(enabledPreference());
        lenient().when(koreaClock.now()).thenReturn(LocalTime.of(3, 0));

        service.onCertificationApproved(new CertificationApprovedEvent(1L, "펜타포트", "Pentaport", 10L));

        then(deviceTokenRepository).should(never()).findTokensWithLanguageByUserIds(any());
        then(pendingPushRepository).should().save(argThat(p ->
                p.getType() == NotificationType.CERT_APPROVED && p.getUserIds().equals(List.of(1L))));
    }

    @Test
    void 낮시간_인증승인알림은_즉시발송() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(festivalRepository.findById(10L)).willReturn(Optional.of(Festival.builder().id(10L).title("펜타포트").build()));
        given(preferenceService.getOrCreate(1L)).willReturn(enabledPreference());

        service.onCertificationApproved(new CertificationApprovedEvent(1L, "펜타포트", "Pentaport", 10L));

        then(deviceTokenRepository).should().findTokensWithLanguageByUserIds(List.of(1L));
        then(pendingPushRepository).should(never()).save(any());
    }

    @Test
    void 새벽시간_좋아요알림은_예외로_즉시발송() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(preferenceService.getOrCreate(1L)).willReturn(enabledPreference());
        lenient().when(koreaClock.now()).thenReturn(LocalTime.of(3, 0));

        service.onPostLiked(new PostLikedEvent(1L, "좋아요러", "제목", 5L, 99L));

        then(deviceTokenRepository).should().findTokensWithLanguageByUserIds(List.of(1L));
        then(pendingPushRepository).should(never()).save(any());
    }

    @Test
    void 오전9시_대기열발송_성공건은_삭제되고_실패건은_유지() {
        PendingPush ok = PendingPush.builder()
                .type(NotificationType.NEW_FESTIVAL).title("t").body("b").userIds(List.of(1L)).build();
        PendingPush fail = PendingPush.builder()
                .type(NotificationType.NEW_FESTIVAL).title("t2").body("b2").userIds(List.of(2L)).build();
        TokenLanguageProjection koToken = token("tok1", "ko");
        given(pendingPushRepository.findAllWithRecipients()).willReturn(List.of(ok, fail));
        given(deviceTokenRepository.findTokensWithLanguageByUserIds(List.of(1L))).willReturn(List.of(koToken));
        given(deviceTokenRepository.findTokensWithLanguageByUserIds(List.of(2L)))
                .willThrow(new RuntimeException("device token 조회 실패"));

        service.flushPendingPushes();

        then(fcmPushService).should().sendMulticast(eq(List.of("tok1")), any());
        then(pendingPushRepository).should().delete(ok);
        then(pendingPushRepository).should(never()).delete(fail);
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

        service.onPostLiked(new PostLikedEvent(1L, "좋아요러", "제목", 5L, 99L));

        then(fcmPushService).should().sendMulticast(eq(List.of("ko-tok")), argThat(m -> m.type() == NotificationType.POST_LIKED));
        then(fcmPushService).should().sendMulticast(eq(List.of("en-tok")), argThat(m -> m.type() == NotificationType.POST_LIKED));
    }

    // ── saveAdminBroadcastNotification / saveAdminBroadcastNotifications ───

    @Test
    void 관리자_개별_테스트발송_저장() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));

        service.saveAdminBroadcastNotification(1L, "제목", "내용");

        then(notificationRepository).should().save(any());
    }

    @Test
    void 관리자_개별_테스트발송_유저없으면_예외() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.saveAdminBroadcastNotification(99L, "제목", "내용"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }

    @Test
    void 관리자_타겟발송_일괄_저장() {
        given(userRepository.findAllById(List.of(1L, 2L))).willReturn(List.of(user(1L), user(2L)));

        service.saveAdminBroadcastNotifications(List.of(1L, 2L), "제목", "내용");

        then(notificationRepository).should().saveAll(anyList());
    }
}
