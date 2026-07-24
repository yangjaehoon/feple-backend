package com.feple.feple_backend.festival.scheduler;

import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.festival.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherCollectionScheduler {

    private final FestivalRepository festivalRepository;
    private final WeatherService weatherService;

    /** 매일 오전 8시 실행 — 진행 중이거나 3일 이내 시작 페스티벌의 날씨 수집 */
    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Seoul")
    @SchedulerLock(name = "weatherCollectionScheduler", lockAtMostFor = "10m", lockAtLeastFor = "1m")
    public void collect() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDate until = today.plusDays(WeatherService.FORECAST_LOOKAHEAD_DAYS);

        List<Festival> targets = festivalRepository.findOngoingOrStartingBefore(today, until);
        if (targets.isEmpty()) {
            log.info("[WeatherScheduler] 수집 대상 페스티벌 없음");
            return;
        }

        log.info("[WeatherScheduler] 날씨 수집 시작 — 대상 {}개", targets.size());
        int success = 0, skip = 0, fail = 0;

        for (Festival festival : targets) {
            try {
                boolean fetched = weatherService.collectWeather(festival);
                if (fetched) success++; else skip++;
            } catch (Exception e) {
                log.error("[WeatherScheduler] 수집 실패: festivalId={} title={}",
                        festival.getId(), festival.getTitle(), e);
                fail++;
            }
        }

        log.info("[WeatherScheduler] 완료 — 성공 {}개 / 스킵 {}개 / 실패 {}개", success, skip, fail);
    }
}
