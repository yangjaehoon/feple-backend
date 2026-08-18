package com.feple.feple_backend.notification.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.feple.feple_backend.artistfestival.entity.ArtistFestival;
import com.feple.feple_backend.artistfestival.repository.ArtistFestivalRepository;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalLikeRepository;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.notification.service.NotificationService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FestivalReminderSchedulerTest {

    @Mock FestivalRepository festivalRepository;
    @Mock ArtistFestivalRepository artistFestivalRepository;
    @Mock ArtistFollowRepository artistFollowRepository;
    @Mock FestivalLikeRepository festivalLikeRepository;
    @Mock NotificationService notificationService;

    @InjectMocks FestivalReminderScheduler scheduler;

    @Test
    void 대상_페스티벌_없으면_알림_미발송() {
        given(festivalRepository.findByStartDate(any())).willReturn(List.of());

        scheduler.sendReminders();

        then(notificationService).shouldHaveNoInteractions();
    }

    @Test
    void 참여_아티스트_없으면_알림_미발송() {
        LocalDate dDay7 = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(7);
        LocalDate dDay1 = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(1);
        Festival festival = Festival.builder().id(1L).title("펜타포트").build();
        given(festivalRepository.findByStartDate(dDay7)).willReturn(List.of(festival));
        given(festivalRepository.findByStartDate(dDay1)).willReturn(List.of());
        given(artistFestivalRepository.findByFestivalIdInWithArtist(List.of(1L))).willReturn(List.of());

        scheduler.sendReminders();

        then(notificationService).shouldHaveNoInteractions();
    }

    @Test
    void 팔로워_없으면_알림_미발송() {
        LocalDate dDay7 = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(7);
        LocalDate dDay1 = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(1);
        Festival festival = Festival.builder().id(1L).title("펜타포트").build();
        given(festivalRepository.findByStartDate(dDay7)).willReturn(List.of(festival));
        given(festivalRepository.findByStartDate(dDay1)).willReturn(List.of());

        ArtistFestival af = mock(ArtistFestival.class);
        given(af.getFestivalId()).willReturn(1L);
        given(af.getArtistId()).willReturn(10L);
        given(artistFestivalRepository.findByFestivalIdInWithArtist(List.of(1L))).willReturn(List.of(af));
        given(artistFollowRepository.findArtistIdAndUserIdByArtistIdIn(List.of(10L))).willReturn(List.of());

        scheduler.sendReminders();

        then(notificationService).shouldHaveNoInteractions();
    }

    @Test
    void 정상_케이스면_D7_D1_각각_알림_발송() {
        LocalDate dDay7 = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(7);
        LocalDate dDay1 = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(1);
        Festival festival = Festival.builder().id(1L).title("펜타포트").titleEn("Pentaport").build();
        given(festivalRepository.findByStartDate(dDay7)).willReturn(List.of(festival));
        given(festivalRepository.findByStartDate(dDay1)).willReturn(List.of(festival));

        ArtistFestival af = mock(ArtistFestival.class);
        given(af.getFestivalId()).willReturn(1L);
        given(af.getArtistId()).willReturn(10L);
        given(artistFestivalRepository.findByFestivalIdInWithArtist(List.of(1L))).willReturn(List.of(af));
        given(artistFollowRepository.findArtistIdAndUserIdByArtistIdIn(List.of(10L)))
                .willReturn(List.of(new Object[]{10L, 100L}, new Object[]{10L, 200L}));

        scheduler.sendReminders();

        then(notificationService).should().sendFestivalReminders(1L, "펜타포트", "Pentaport", List.of(100L, 200L), 7);
        then(notificationService).should().sendFestivalReminders(1L, "펜타포트", "Pentaport", List.of(100L, 200L), 1);
    }

    @Test
    void 아티스트는_안_팔로우해도_페스티벌을_찜했으면_알림_발송() {
        LocalDate dDay7 = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(7);
        LocalDate dDay1 = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(1);
        Festival festival = Festival.builder().id(1L).title("펜타포트").titleEn("Pentaport").build();
        given(festivalRepository.findByStartDate(dDay7)).willReturn(List.of(festival));
        given(festivalRepository.findByStartDate(dDay1)).willReturn(List.of());

        // 라인업 아티스트는 있지만 아무도 팔로우하지 않음
        ArtistFestival af = mock(ArtistFestival.class);
        given(af.getFestivalId()).willReturn(1L);
        given(af.getArtistId()).willReturn(10L);
        given(artistFestivalRepository.findByFestivalIdInWithArtist(List.of(1L))).willReturn(List.of(af));
        given(artistFollowRepository.findArtistIdAndUserIdByArtistIdIn(List.of(10L))).willReturn(List.of());
        // 대신 페스티벌 자체를 찜한 유저가 있음
        given(festivalLikeRepository.findFestivalIdAndUserIdByFestivalIdIn(List.of(1L)))
                .willReturn(List.<Object[]>of(new Object[]{1L, 300L}));

        scheduler.sendReminders();

        then(notificationService).should().sendFestivalReminders(1L, "펜타포트", "Pentaport", List.of(300L), 7);
    }

    @Test
    void 라인업_아티스트가_없는_축제도_찜한_유저에게는_알림_발송() {
        LocalDate dDay7 = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(7);
        LocalDate dDay1 = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(1);
        Festival festival = Festival.builder().id(1L).title("펜타포트").titleEn("Pentaport").build();
        given(festivalRepository.findByStartDate(dDay7)).willReturn(List.of(festival));
        given(festivalRepository.findByStartDate(dDay1)).willReturn(List.of());
        given(artistFestivalRepository.findByFestivalIdInWithArtist(List.of(1L))).willReturn(List.of());
        given(festivalLikeRepository.findFestivalIdAndUserIdByFestivalIdIn(List.of(1L)))
                .willReturn(List.<Object[]>of(new Object[]{1L, 300L}));

        scheduler.sendReminders();

        then(notificationService).should().sendFestivalReminders(1L, "펜타포트", "Pentaport", List.of(300L), 7);
    }

    @Test
    void 팔로워와_찜한_유저가_겹치면_중복없이_한번만_발송대상에_포함() {
        LocalDate dDay7 = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(7);
        LocalDate dDay1 = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(1);
        Festival festival = Festival.builder().id(1L).title("펜타포트").titleEn("Pentaport").build();
        given(festivalRepository.findByStartDate(dDay7)).willReturn(List.of(festival));
        given(festivalRepository.findByStartDate(dDay1)).willReturn(List.of());

        ArtistFestival af = mock(ArtistFestival.class);
        given(af.getFestivalId()).willReturn(1L);
        given(af.getArtistId()).willReturn(10L);
        given(artistFestivalRepository.findByFestivalIdInWithArtist(List.of(1L))).willReturn(List.of(af));
        given(artistFollowRepository.findArtistIdAndUserIdByArtistIdIn(List.of(10L)))
                .willReturn(List.<Object[]>of(new Object[]{10L, 100L}));
        // 100번 유저가 아티스트도 팔로우하고 페스티벌도 찜함
        given(festivalLikeRepository.findFestivalIdAndUserIdByFestivalIdIn(List.of(1L)))
                .willReturn(List.<Object[]>of(new Object[]{1L, 100L}));

        scheduler.sendReminders();

        then(notificationService).should().sendFestivalReminders(1L, "펜타포트", "Pentaport", List.of(100L), 7);
    }
}
