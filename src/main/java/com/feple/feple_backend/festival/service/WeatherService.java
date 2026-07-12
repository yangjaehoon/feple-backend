package com.feple.feple_backend.festival.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.feple.feple_backend.festival.dto.WeatherDto;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.entity.FestivalWeather;
import com.feple.feple_backend.festival.entity.Region;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.festival.repository.FestivalWeatherRepository;
import com.feple.feple_backend.global.EntityLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    private static final Map<Region, int[]> REGION_GRID = Map.ofEntries(
            Map.entry(Region.SEOUL,     new int[]{60, 127}),
            Map.entry(Region.BUSAN,     new int[]{98, 76}),
            Map.entry(Region.INCHEON,   new int[]{55, 124}),
            Map.entry(Region.DAEGU,     new int[]{89, 90}),
            Map.entry(Region.DAEJEON,   new int[]{67, 100}),
            Map.entry(Region.GWANGJU,   new int[]{58, 74}),
            Map.entry(Region.ULSAN,     new int[]{102, 84}),
            Map.entry(Region.SEJONG,    new int[]{66, 103}),
            Map.entry(Region.GYEONGGI, new int[]{60, 120}),
            Map.entry(Region.GANGWON,   new int[]{73, 134}),
            Map.entry(Region.CHUNGBUK,  new int[]{69, 107}),
            Map.entry(Region.CHUNGNAM,  new int[]{68, 100}),
            Map.entry(Region.GYEONGBUK, new int[]{89, 91}),
            Map.entry(Region.GYEONGNAM, new int[]{91, 77}),
            Map.entry(Region.JEONBUK,   new int[]{63, 89}),
            Map.entry(Region.JEONNAM,   new int[]{51, 67}),
            Map.entry(Region.JEJU,      new int[]{52, 38})
    );

    // 기상청 초단기예보 발표시각(3시간 간격, 고정값) — 임의 조정 불가
    private static final int[] BASE_HOURS = {2, 5, 8, 11, 14, 17, 20, 23};
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String KMA_SUCCESS_CODE = "00";
    private static final String NOON_FORECAST_TIME = "1200";
    private static final String EARLY_MORNING_BASE_TIME = "0200";
    private static final String DEFAULT_SKY_CODE = "1";
    private static final String DEFAULT_PTY_CODE = "0";

    @Value("${kma.service-key}")
    private String serviceKey;

    @Value("${kma.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final FestivalRepository festivalRepository;
    private final FestivalWeatherRepository weatherRepository;

    /**
     * 스케줄러 전용: API에서 날씨를 수집해 DB에 저장.
     * 종료된 페스티벌·API 키 미설정·날짜 범위 초과 시 false 반환(정상 스킵).
     * API 호출 실패 시 예외를 그대로 전파.
     * 외부 API 호출(fetchFromApi)이 커넥션을 물고 있지 않도록 트랜잭션은 DB 저장(saveOrUpdate)에만 건다.
     */
    public boolean collectWeather(Festival festival) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDate end = festival.getEndDate() != null ? festival.getEndDate() : festival.getStartDate();
        if (end != null && end.isBefore(today)) return false;

        if (serviceKey == null || serviceKey.isBlank()) return false;

        LocalDate targetDate = resolveTargetDate(festival, today);
        if (targetDate == null) return false;

        String[] baseDatetime = resolveBaseDatetime(targetDate, today);
        if (baseDatetime == null) return false;

        int[] grid = resolveGrid(festival);
        WeatherDto dto = fetchFromApi(grid, targetDate, baseDatetime);
        saveOrUpdate(festival, dto);
        return true;
    }

    /** 컨트롤러 전용: API 실패 시 캐시 데이터로 폴백. */
    public Optional<WeatherDto> getByFestivalId(Long festivalId) {
        Festival festival = EntityLoader.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDate end = festival.getEndDate() != null ? festival.getEndDate() : festival.getStartDate();
        if (end != null && end.isBefore(today)) {
            return weatherRepository.findByFestivalId(festivalId).map(FestivalWeather::toDto);
        }

        try {
            collectWeather(festival);
        } catch (Exception e) {
            log.error("기상청 API 호출 실패: festivalId={}", festivalId, e);
        }
        return weatherRepository.findByFestivalId(festivalId).map(FestivalWeather::toDto);
    }

    private void saveOrUpdate(Festival festival, WeatherDto dto) {
        FestivalWeather weather = weatherRepository.findByFestivalId(festival.getId())
                .orElse(FestivalWeather.of(festival, dto));
        weather.apply(dto);
        weatherRepository.save(weather);
    }

    private WeatherDto fetchFromApi(int[] grid, LocalDate targetDate, String[] baseDatetime) {
        var uri = UriComponentsBuilder.fromUriString(baseUrl + "/getVilageFcst")
                .queryParam("serviceKey", serviceKey)
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 1000)
                .queryParam("dataType", "JSON")
                .queryParam("base_date", baseDatetime[0])
                .queryParam("base_time", baseDatetime[1])
                .queryParam("nx", grid[0])
                .queryParam("ny", grid[1])
                .build().toUri();

        JsonNode body = restTemplate.getForObject(uri, JsonNode.class);

        String resultCode = body.path("response").path("header").path("resultCode").asText();
        if (!KMA_SUCCESS_CODE.equals(resultCode)) {
            throw new IllegalStateException("기상청 API 오류: " + resultCode);
        }

        JsonNode items = body.path("response").path("body").path("items").path("item");
        return parseWeather(items, targetDate.format(DATE_FMT));
    }

    private WeatherDto parseWeather(JsonNode items, String fcstDate) {
        double minTemp = Double.MAX_VALUE;
        double maxTemp = -Double.MAX_VALUE;
        int maxRainProb = 0;
        String skyCode = DEFAULT_SKY_CODE;
        String ptyCode = DEFAULT_PTY_CODE;
        boolean hasTmn = false, hasTmx = false;

        for (JsonNode item : items) {
            if (!fcstDate.equals(item.path("fcstDate").asText())) continue;
            String category = item.path("category").asText();
            String value = item.path("fcstValue").asText();
            String fcstTime = item.path("fcstTime").asText();

            switch (category) {
                case "TMN" -> { minTemp = Double.parseDouble(value); hasTmn = true; }
                case "TMX" -> { maxTemp = Double.parseDouble(value); hasTmx = true; }
                case "POP" -> maxRainProb = Math.max(maxRainProb, Integer.parseInt(value));
                case "SKY" -> { if (NOON_FORECAST_TIME.equals(fcstTime)) skyCode = value; }
                case "PTY" -> {
                    int next = Integer.parseInt(value);
                    if (next > Integer.parseInt(ptyCode)) ptyCode = value;
                }
            }
        }

        if (!hasTmn || !hasTmx) {
            for (JsonNode item : items) {
                if (!fcstDate.equals(item.path("fcstDate").asText())) continue;
                if (!"TMP".equals(item.path("category").asText())) continue;
                double fcstTemp = Double.parseDouble(item.path("fcstValue").asText());
                if (!hasTmn && fcstTemp < minTemp) minTemp = fcstTemp;
                if (!hasTmx && fcstTemp > maxTemp) maxTemp = fcstTemp;
            }
        }

        return new WeatherDto(
                fcstDate,
                minTemp == Double.MAX_VALUE ? 0 : minTemp,
                maxTemp == -Double.MAX_VALUE ? 0 : maxTemp,
                maxRainProb,
                skyCode,
                ptyCode
        );
    }

    private LocalDate resolveTargetDate(Festival festival, LocalDate today) {
        if (festival.getStartDate() == null) return null;
        LocalDate start = festival.getStartDate();
        LocalDate end = festival.getEndDate() != null ? festival.getEndDate() : start;

        if (!today.isBefore(start) && !today.isAfter(end)) return today;
        if (today.isBefore(start)) return start;
        return end;
    }

    private int[] resolveGrid(Festival festival) {
        if (festival.getLatitude() != null && festival.getLongitude() != null) {
            return WeatherGridConverter.toGrid(festival.getLatitude(), festival.getLongitude());
        }
        return REGION_GRID.getOrDefault(festival.getRegion(), new int[]{60, 127});
    }

    private String[] resolveBaseDatetime(LocalDate targetDate, LocalDate today) {
        // 기상청 API 조회 가능 범위: 과거 2일 ~ 미래 3일
        if (targetDate.isBefore(today.minusDays(2))) return null;
        if (targetDate.isAfter(today.plusDays(3))) return null;

        if (targetDate.isBefore(today)) {
            return new String[]{targetDate.format(DATE_FMT), EARLY_MORNING_BASE_TIME};
        }

        // 기상청 API는 발표시각 이후 약 10분 뒤 데이터가 올라옴 — 그 전에 조회하면 최신 시간대 누락
        LocalTime nowKST = LocalTime.now(ZoneId.of("Asia/Seoul")).minusMinutes(10);
        int currentHour = nowKST.getHour();
        LocalDate apiDate = today;
        int bestHour = 2;

        if (currentHour < 2) {
            apiDate = today.minusDays(1);
            bestHour = 23;
        } else {
            for (int h : BASE_HOURS) {
                if (h <= currentHour) bestHour = h;
            }
        }
        return new String[]{apiDate.format(DATE_FMT), String.format("%02d00", bestHour)};
    }
}
