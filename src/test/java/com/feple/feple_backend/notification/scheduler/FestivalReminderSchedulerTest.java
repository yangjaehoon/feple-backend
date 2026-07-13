package com.feple.feple_backend.notification.scheduler;

import com.feple.feple_backend.artistfestival.entity.ArtistFestival;
import com.feple.feple_backend.artistfestival.repository.ArtistFestivalRepository;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class FestivalReminderSchedulerTest {

    @Mock FestivalRepository festivalRepository;
    @Mock ArtistFestivalRepository artistFestivalRepository;
    @Mock ArtistFollowRepository artistFollowRepository;
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
        LocalDate dDay7 = LocalDate.now().plusDays(7);
        LocalDate dDay1 = LocalDate.now().plusDays(1);
        Festival festival = Festival.builder().id(1L).title("펜타포트").build();
        given(festivalRepository.findByStartDate(dDay7)).willReturn(List.of(festival));
        given(festivalRepository.findByStartDate(dDay1)).willReturn(List.of());
        given(artistFestivalRepository.findByFestivalIdInWithArtist(List.of(1L))).willReturn(List.of());

        scheduler.sendReminders();

        then(notificationService).shouldHaveNoInteractions();
    }

    @Test
    void 팔로워_없으면_알림_미발송() {
        LocalDate dDay7 = LocalDate.now().plusDays(7);
        LocalDate dDay1 = LocalDate.now().plusDays(1);
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
        LocalDate dDay7 = LocalDate.now().plusDays(7);
        LocalDate dDay1 = LocalDate.now().plusDays(1);
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
}
