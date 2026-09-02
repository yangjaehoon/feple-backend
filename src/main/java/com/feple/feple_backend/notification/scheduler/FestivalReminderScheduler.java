package com.feple.feple_backend.notification.scheduler;

import com.feple.feple_backend.artistfestival.entity.ArtistFestival;
import com.feple.feple_backend.artistfestival.repository.ArtistFestivalRepository;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalLikeRepository;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.global.KoreaClock;
import com.feple.feple_backend.notification.service.NotificationService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FestivalReminderScheduler {

    private final KoreaClock koreaClock;
    private final FestivalRepository festivalRepository;
    private final ArtistFestivalRepository artistFestivalRepository;
    private final ArtistFollowRepository artistFollowRepository;
    private final FestivalLikeRepository festivalLikeRepository;
    private final NotificationService notificationService;

    /** 매일 오전 9시(KST) 실행 */
    @Scheduled(cron = "0 0 9 * * *", zone = KoreaClock.ZONE_ID)
    @SchedulerLock(name = "festivalReminderScheduler", lockAtMostFor = "5m", lockAtLeastFor = "1m")
    public void sendReminders() {
        // D-7 처리 중 예외가 나도 D-1 리마인더는 별도로 발송돼야 함
        try {
            sendReminderForDDay(7);
        } catch (Exception e) {
            log.error("[ReminderScheduler] D-7 리마인더 처리 실패", e);
        }
        try {
            sendReminderForDDay(1);
        } catch (Exception e) {
            log.error("[ReminderScheduler] D-1 리마인더 처리 실패", e);
        }
    }

    private void sendReminderForDDay(int dDay) {
        LocalDate targetDate = koreaClock.today().plusDays(dDay);
        List<Festival> festivals = festivalRepository.findByStartDate(targetDate);

        if (festivals.isEmpty()) return;
        log.info("[ReminderScheduler] D-{} 대상 페스티벌 {}개", dDay, festivals.size());

        List<Long> festivalIds = festivals.stream().map(Festival::getId).toList();
        Map<Long, List<Long>> artistIdsByFestivalId = buildArtistIdsByFestival(festivalIds);

        List<Long> allArtistIds = artistIdsByFestivalId.values().stream()
                .flatMap(List::stream)
                .distinct()
                .toList();
        // 아티스트 라인업이 없는 축제라도 페스티벌 자체를 찜한 유저는 리마인더 대상이라
        // 여기서 전체 리턴하면 안 됨 — userIdsByArtistId만 비워두고 계속 진행.
        Map<Long, List<Long>> userIdsByArtistId =
                allArtistIds.isEmpty() ? Map.of() : buildUserIdsByArtist(allArtistIds);
        Map<Long, List<Long>> likedUserIdsByFestivalId = buildLikedUserIdsByFestival(festivalIds);

        for (Festival festival : festivals) {
            dispatchReminder(festival, dDay, artistIdsByFestivalId, userIdsByArtistId, likedUserIdsByFestivalId);
        }
    }

    private Map<Long, List<Long>> buildArtistIdsByFestival(List<Long> festivalIds) {
        return artistFestivalRepository
                .findByFestivalIdInWithArtist(festivalIds)
                .stream()
                .collect(Collectors.groupingBy(ArtistFestival::getFestivalId,
                        Collectors.mapping(ArtistFestival::getArtistId, Collectors.toList())));
    }

    private Map<Long, List<Long>> buildUserIdsByArtist(List<Long> artistIds) {
        return artistFollowRepository
                .findArtistIdAndUserIdByArtistIdIn(artistIds)
                .stream()
                .collect(Collectors.groupingBy(row -> (Long) row[0],
                        Collectors.mapping(row -> (Long) row[1], Collectors.toList())));
    }

    // 아티스트 팔로우 없이 페스티벌 자체만 찜한 유저도 리마인더 대상에 포함시키기 위함
    private Map<Long, List<Long>> buildLikedUserIdsByFestival(List<Long> festivalIds) {
        return festivalLikeRepository
                .findFestivalIdAndUserIdByFestivalIdIn(festivalIds)
                .stream()
                .collect(Collectors.groupingBy(row -> (Long) row[0],
                        Collectors.mapping(row -> (Long) row[1], Collectors.toList())));
    }

    private void dispatchReminder(Festival festival, int dDay,
                                   Map<Long, List<Long>> artistIdsByFestivalId,
                                   Map<Long, List<Long>> userIdsByArtistId,
                                   Map<Long, List<Long>> likedUserIdsByFestivalId) {
        List<Long> artistIds = artistIdsByFestivalId.getOrDefault(festival.getId(), List.of());
        Stream<Long> followerUserIds = artistIds.stream()
                .flatMap(artistId -> userIdsByArtistId.getOrDefault(artistId, List.of()).stream());
        Stream<Long> likedUserIds = likedUserIdsByFestivalId.getOrDefault(festival.getId(), List.of()).stream();

        List<Long> userIds = Stream.concat(followerUserIds, likedUserIds).distinct().toList();
        if (userIds.isEmpty()) return;

        // 한 페스티벌에서 예외가 나도 나머지 페스티벌의 리마인더는 계속 발송돼야 함
        try {
            notificationService.sendFestivalReminders(
                    festival.getId(), festival.getTitle(), festival.getTitleEn(), userIds, dDay);
        } catch (Exception e) {
            log.error("[ReminderScheduler] D-{} 리마인더 발송 실패: festivalId={}", dDay, festival.getId(), e);
        }
    }
}
