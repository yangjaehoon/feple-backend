package com.feple.feple_backend.notification.scheduler;

import com.feple.feple_backend.artistfestival.entity.ArtistFestival;
import com.feple.feple_backend.artistfestival.repository.ArtistFestivalRepository;
import com.feple.feple_backend.artistfollow.entity.ArtistFollow;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class FestivalReminderScheduler {

    private final FestivalRepository festivalRepository;
    private final ArtistFestivalRepository artistFestivalRepository;
    private final ArtistFollowRepository artistFollowRepository;
    private final NotificationService notificationService;

    /** 매일 오전 9시 실행 */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendReminders() {
        sendReminderForDDay(7);
        sendReminderForDDay(1);
    }

    private void sendReminderForDDay(int dDay) {
        LocalDate targetDate = LocalDate.now().plusDays(dDay);
        List<Festival> festivals = festivalRepository.findByStartDate(targetDate);

        if (festivals.isEmpty()) return;
        log.info("[ReminderScheduler] D-{} 대상 페스티벌 {}개", dDay, festivals.size());

        for (Festival festival : festivals) {
            // 해당 페스티벌에 출연하는 아티스트 조회
            List<Long> artistIds = artistFestivalRepository
                    .findByFestivalIdOrderByLineupOrderAsc(festival.getId())
                    .stream()
                    .map(af -> af.getArtist().getId())
                    .toList();

            if (artistIds.isEmpty()) continue;

            // 해당 아티스트들을 팔로우하는 유저 (중복 제거)
            List<Long> userIds = artistIds.stream()
                    .flatMap(artistId ->
                            artistFollowRepository.findByArtistId(artistId)
                                    .stream()
                                    .map(follow -> follow.getUser().getId()))
                    .distinct()
                    .collect(Collectors.toList());

            if (userIds.isEmpty()) continue;

            notificationService.sendFestivalReminders(
                    festival.getId(), festival.getTitle(), userIds, dDay);
        }
    }
}
