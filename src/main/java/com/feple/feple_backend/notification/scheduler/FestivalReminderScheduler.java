package com.feple.feple_backend.notification.scheduler;

import com.feple.feple_backend.artistfestival.entity.ArtistFestival;
import com.feple.feple_backend.artistfestival.repository.ArtistFestivalRepository;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
    @SchedulerLock(name = "festivalReminderScheduler", lockAtMostFor = "5m", lockAtLeastFor = "1m")
    public void sendReminders() {
        sendReminderForDDay(7);
        sendReminderForDDay(1);
    }

    private void sendReminderForDDay(int dDay) {
        LocalDate targetDate = LocalDate.now().plusDays(dDay);
        List<Festival> festivals = festivalRepository.findByStartDate(targetDate);

        if (festivals.isEmpty()) return;
        log.info("[ReminderScheduler] D-{} 대상 페스티벌 {}개", dDay, festivals.size());

        List<Long> festivalIds = festivals.stream().map(Festival::getId).toList();
        Map<Long, List<Long>> artistIdsByFestivalId = artistFestivalRepository
                .findByFestivalIdInWithArtist(festivalIds)
                .stream()
                .collect(Collectors.groupingBy(ArtistFestival::getFestivalId,
                        Collectors.mapping(ArtistFestival::getArtistId, Collectors.toList())));

        List<Long> allArtistIds = artistIdsByFestivalId.values().stream()
                .flatMap(List::stream)
                .distinct()
                .toList();
        if (allArtistIds.isEmpty()) return;

        Map<Long, List<Long>> userIdsByArtistId = artistFollowRepository
                .findArtistIdAndUserIdByArtistIdIn(allArtistIds)
                .stream()
                .collect(Collectors.groupingBy(row -> (Long) row[0],
                        Collectors.mapping(row -> (Long) row[1], Collectors.toList())));

        for (Festival festival : festivals) {
            List<Long> artistIds = artistIdsByFestivalId.getOrDefault(festival.getId(), List.of());
            if (artistIds.isEmpty()) continue;

            List<Long> userIds = artistIds.stream()
                    .flatMap(artistId -> userIdsByArtistId.getOrDefault(artistId, List.of()).stream())
                    .distinct()
                    .toList();
            if (userIds.isEmpty()) continue;

            notificationService.sendFestivalReminders(
                    festival.getId(), festival.getTitle(), festival.getTitleEn(), userIds, dDay);
        }
    }
}
