package com.feple.feple_backend.festival.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.feple.feple_backend.festival.dto.WeatherDto;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.entity.FestivalWeather;
import com.feple.feple_backend.festival.entity.Region;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.festival.repository.FestivalWeatherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.NoSuchElementException;
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

    private static final int[] BASE_HOURS = {2, 5, 8, 11, 14, 17, 20, 23};
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Value("${kma.service-key}")
    private String serviceKey;

    @Value("${kma.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final FestivalRepository festivalRepository;
    private final FestivalWeatherRepository weatherRepository;

    @Transactional
    public Optional<WeatherDto> getByFestivalId(Long festivalId) {
        Festival festival = festivalRepository.findById(festivalId)
                .orElseThrow(() -> new NoSuchElementException("페스티벌"));

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDate end = festival.getEndDate() != null ? festival.getEndDate() : festival.getStartDate();
        boolean isEnded = end != null && end.isBefore(today);

        // 종료된 페스티벌: DB에 저장된 날씨만 반환
        if (isEnded) {
            return weatherRepository.findByFestivalId(festivalId).map(FestivalWeather::toDto);
        }

        // 진행 중 / 예정: API 호출 후 DB에 저장
        if (serviceKey == null || serviceKey.isBlank()) {
            return weatherRepository.findByFestivalId(festivalId).map(FestivalWeather::toDto);
        }

        LocalDate targetDate = resolveTargetDate(festival, today);
        if (targetDate == null) return Optional.empty();

        String[] baseDatetime = resolveBaseDatetime(targetDate, today);
        if (baseDatetime == null) return Optional.empty();

        int[] grid = resolveGrid(festival);

        try {
            WeatherDto dto = fetchFromApi(grid[0], grid[1], targetDate, baseDatetime);
            saveOrUpdate(festivalId, dto);
            return Optional.of(dto);
        } catch (Exception e) {
            log.error("기상청 API 호출 실패: festivalId={}", festivalId, e);
            return weatherRepository.findByFestivalId(festivalId).map(FestivalWeather::toDto);
        }
    }

    private void saveOrUpdate(Long festivalId, WeatherDto dto) {
        FestivalWeather weather = weatherRepository.findByFestivalId(festivalId)
                .orElse(FestivalWeather.of(festivalId, dto));
        weather.apply(dto);
        weatherRepository.save(weather);
    }

    private WeatherDto fetchFromApi(int nx, int ny, LocalDate targetDate, String[] baseDatetime) {
        String url = baseUrl + "/getVilageFcst"
                + "?serviceKey=" + serviceKey
                + "&pageNo=1&numOfRows=1000&dataType=JSON"
                + "&base_date=" + baseDatetime[0]
                + "&base_time=" + baseDatetime[1]
                + "&nx=" + nx + "&ny=" + ny;

        JsonNode body = restTemplate.getForObject(URI.create(url), JsonNode.class);
        JsonNode items = body.path("response").path("body").path("items").path("item");

        String resultCode = body.path("response").path("header").path("resultCode").asText();
        if (!"00".equals(resultCode)) {
            throw new IllegalStateException("기상청 API 오류: " + resultCode);
        }

        return parseWeather(items, targetDate.format(DATE_FMT));
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
                case "SKY" -> { if ("1200".equals(fcstTime)) skyCode = value; }
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
                maxTemp == Double.MIN_VALUE ? 0 : maxTemp,
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
            return KmaGridConverter.toGrid(festival.getLatitude(), festival.getLongitude());
        }
        return REGION_GRID.getOrDefault(festival.getRegion(), new int[]{60, 127});
    }

    private String[] resolveBaseDatetime(LocalDate targetDate, LocalDate today) {
        if (targetDate.isBefore(today.minusDays(2))) return null;
        if (targetDate.isAfter(today.plusDays(3))) return null;

        if (targetDate.isBefore(today)) {
            return new String[]{targetDate.format(DATE_FMT), "0200"};
        }

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
