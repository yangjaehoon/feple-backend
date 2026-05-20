package com.feple.feple_backend.festival.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.feple.feple_backend.festival.dto.WeatherDto;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.entity.Region;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    // 지역별 기상청 격자 좌표 (위경도 없는 페스티벌 fallback)
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

    private static final int[] BASE_HOURS = {2, 5, 8, 11, 14, 17, 20, 23};
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Value("${kma.service-key}")
    private String serviceKey;

    @Value("${kma.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final FestivalRepository festivalRepository;

    public Optional<WeatherDto> getByFestivalId(Long festivalId) {
        Festival festival = festivalRepository.findById(festivalId)
                .orElseThrow(() -> new NoSuchElementException("페스티벌"));

        if (serviceKey == null || serviceKey.isBlank()) return Optional.empty();

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDate targetDate = resolveTargetDate(festival, today);
        if (targetDate == null) return Optional.empty();

        String[] baseDatetime = resolveBaseDatetime(targetDate, today);
        if (baseDatetime == null) return Optional.empty(); // API 범위 초과

        int[] grid = resolveGrid(festival);
        String cacheKey = grid[0] + "_" + grid[1] + "_" + targetDate.format(DATE_FMT);

        try {
            return Optional.of(fetchWeather(cacheKey, grid[0], grid[1], targetDate, baseDatetime));
        } catch (Exception e) {
            log.error("기상청 API 호출 실패: festivalId={}", festivalId, e);
            return Optional.empty();
        }
    }

    @Cacheable(value = "weather", key = "#cacheKey")
    public WeatherDto fetchWeather(String cacheKey, int nx, int ny, LocalDate targetDate, String[] baseDatetime) {
        String apiBaseDate = baseDatetime[0];
        String apiBaseTime = baseDatetime[1];

        // serviceKey는 이미 인코딩된 키를 그대로 사용 (재인코딩 방지)
        String url = baseUrl + "/getVilageFcst"
                + "?serviceKey=" + serviceKey
                + "&pageNo=1&numOfRows=1000&dataType=JSON"
                + "&base_date=" + apiBaseDate
                + "&base_time=" + apiBaseTime
                + "&nx=" + nx + "&ny=" + ny;

        URI uri = URI.create(url);

        JsonNode body = restTemplate.getForObject(uri, JsonNode.class);
        JsonNode items = body.path("response").path("body").path("items").path("item");

        String fcstDate = targetDate.format(DATE_FMT);
        return parseWeather(items, fcstDate);
    }

    private WeatherDto parseWeather(JsonNode items, String fcstDate) {
        double minTemp = Double.MAX_VALUE;
        double maxTemp = Double.MIN_VALUE;
        int maxRainProb = 0;
        String skyCode = "1";
        String ptyCode = "0";
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
                // 정오 하늘 상태
                case "SKY" -> { if ("1200".equals(fcstTime)) skyCode = value; }
                // 가장 강한 강수 형태 우선
                case "PTY" -> {
                    int cur = Integer.parseInt(ptyCode);
                    int next = Integer.parseInt(value);
                    if (next > cur) ptyCode = value;
                }
            }
        }

        // TMN/TMX가 없으면 TMP(시간별 기온)에서 대체
        if (!hasTmn || !hasTmx) {
            for (JsonNode item : items) {
                if (!fcstDate.equals(item.path("fcstDate").asText())) continue;
                if (!"TMP".equals(item.path("category").asText())) continue;
                double tmp = Double.parseDouble(item.path("fcstValue").asText());
                if (!hasTmn && tmp < minTemp) minTemp = tmp;
                if (!hasTmx && tmp > maxTemp) maxTemp = tmp;
            }
        }

        return new WeatherDto(
                fcstDate,
                minTemp == Double.MAX_VALUE ? 0 : minTemp,
                maxTemp == Double.MIN_VALUE ? 0 : maxTemp,
                maxRainProb,
                skyCode,
                ptyCode
        );
    }

    // 페스티벌 날짜 기준 target date 선택
    // - 진행 중: 오늘
    // - 시작 전: 시작일
    // - 종료됨: 종료일 (API 범위 초과 여부는 resolveBaseDatetime에서 판단)
    private LocalDate resolveTargetDate(Festival festival, LocalDate today) {
        if (festival.getStartDate() == null) return null;
        LocalDate start = festival.getStartDate();
        LocalDate end = festival.getEndDate() != null ? festival.getEndDate() : start;

        if (!today.isBefore(start) && !today.isAfter(end)) return today; // 진행 중
        if (today.isBefore(start)) return start;                          // 시작 전
        return end;                                                        // 종료됨
    }

    private int[] resolveGrid(Festival festival) {
        if (festival.getLatitude() != null && festival.getLongitude() != null) {
            return KmaGridConverter.toGrid(festival.getLatitude(), festival.getLongitude());
        }
        return REGION_GRID.getOrDefault(festival.getRegion(), new int[]{60, 127}); // 기본: 서울
    }

    // 기상청 단기예보 API 제약:
    //   base_date는 최근 3일치만 유효, 각 예보는 base_date 기준 +3일까지 제공
    // - 과거 2일 이내: base_date = targetDate, base_time = "0200" (그날 이른 발표본)
    // - 오늘/미래 3일 이내: base_date = 오늘, base_time = 최신 발표 시각
    // - 그 외(더 먼 과거 or 더 먼 미래): null 반환 → 날씨 숨김
    private String[] resolveBaseDatetime(LocalDate targetDate, LocalDate today) {
        if (targetDate.isBefore(today.minusDays(2))) return null; // 3일+ 전 종료 페스티벌
        if (targetDate.isAfter(today.plusDays(3))) return null;   // 4일+ 뒤 시작 페스티벌

        if (targetDate.isBefore(today)) {
            // 어제/그제 종료된 페스티벌 → 해당 날 02시 발표본 사용
            return new String[]{targetDate.format(DATE_FMT), "0200"};
        }

        // 오늘 또는 미래 3일 이내 → 오늘의 최신 발표 시각 사용
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
