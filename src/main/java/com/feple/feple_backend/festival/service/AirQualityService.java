package com.feple.feple_backend.festival.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.feple.feple_backend.festival.dto.AirQualityDto;
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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AirQualityService {

    private static final Map<Region, String> SIDO_MAP = Map.ofEntries(
            Map.entry(Region.SEOUL,    "서울"),
            Map.entry(Region.BUSAN,    "부산"),
            Map.entry(Region.INCHEON,  "인천"),
            Map.entry(Region.DAEGU,    "대구"),
            Map.entry(Region.DAEJEON,  "대전"),
            Map.entry(Region.GWANGJU,  "광주"),
            Map.entry(Region.ULSAN,    "울산"),
            Map.entry(Region.SEJONG,   "세종"),
            Map.entry(Region.GYEONGGI, "경기"),
            Map.entry(Region.GANGWON,  "강원"),
            Map.entry(Region.CHUNGBUK, "충북"),
            Map.entry(Region.CHUNGNAM, "충남"),
            Map.entry(Region.GYEONGBUK,"경북"),
            Map.entry(Region.GYEONGNAM,"경남"),
            Map.entry(Region.JEONBUK,  "전북"),
            Map.entry(Region.JEONNAM,  "전남"),
            Map.entry(Region.JEJU,     "제주")
    );

    @Value("${airkorea.service-key}")
    private String serviceKey;

    @Value("${airkorea.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final FestivalRepository festivalRepository;

    public Optional<AirQualityDto> getByFestivalId(Long festivalId) {
        Festival festival = festivalRepository.findById(festivalId)
                .orElseThrow(() -> new NoSuchElementException("페스티벌"));
        String sidoName = SIDO_MAP.get(festival.getRegion());
        if (sidoName == null) return Optional.empty();
        if (serviceKey == null || serviceKey.isBlank()) return Optional.empty();
        try {
            return Optional.of(fetchBySido(sidoName));
        } catch (Exception e) {
            log.error("AirKorea API 호출 실패: sido={}", sidoName, e);
            return Optional.empty();
        }
    }

    @Cacheable(value = "airQuality", key = "#sidoName")
    public AirQualityDto fetchBySido(String sidoName) {
        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl + "/getCtprvnRltmMesureDnsty")
                .queryParam("sidoName", sidoName)
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 1)
                .queryParam("returnType", "json")
                .queryParam("serviceKey", serviceKey)
                .queryParam("ver", "1.0")
                .build()
                .encode()
                .toUri();

        JsonNode body = restTemplate.getForObject(uri, JsonNode.class);
        JsonNode items = body.path("response").path("body").path("items");
        if (!items.isArray() || items.size() == 0) {
            throw new IllegalStateException("AirKorea 응답 항목 없음: " + sidoName);
        }
        JsonNode item = items.get(0);
        return new AirQualityDto(
                item.path("stationName").asText("-"),
                sidoName,
                item.path("pm10Value").asText("-"),
                item.path("pm25Value").asText("-"),
                item.path("pm10Grade").asText("-"),
                item.path("pm25Grade").asText("-"),
                item.path("khaiValue").asText("-"),
                item.path("khaiGrade").asText("-"),
                item.path("dataTime").asText("-")
        );
    }
}
