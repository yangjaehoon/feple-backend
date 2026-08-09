package com.feple.feple_backend.festival.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feple.feple_backend.festival.dto.WeatherDto;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.entity.FestivalWeather;
import com.feple.feple_backend.festival.entity.PtyCode;
import com.feple.feple_backend.festival.entity.Region;
import com.feple.feple_backend.festival.entity.SkyCode;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.festival.repository.FestivalWeatherRepository;
import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    private static final DateTimeFormatter KMA_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock RestTemplate restTemplate;
    @Mock FestivalRepository festivalRepository;
    @Mock FestivalWeatherRepository weatherRepository;

    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        weatherService = new WeatherService(restTemplate, festivalRepository, weatherRepository,
                new FestivalWeatherStore(weatherRepository));
        ReflectionTestUtils.setField(weatherService, "serviceKey", "test-key");
        ReflectionTestUtils.setField(weatherService, "baseUrl", "https://apis.data.go.kr/kma");
    }

    private LocalDate today() {
        return LocalDate.now(ZoneId.of("Asia/Seoul"));
    }

    private Festival festival(Long id, LocalDate start, LocalDate end) {
        return Festival.builder().id(id).startDate(start).endDate(end).region(Region.SEOUL).build();
    }

    private JsonNode successBody(String itemsJson) throws Exception {
        String json = "{\"response\":{\"header\":{\"resultCode\":\"00\"},\"body\":{\"items\":{\"item\":" + itemsJson + "}}}}";
        return MAPPER.readTree(json);
    }

    private String item(String category, String fcstDate, String fcstTime, String value) {
        return String.format(
                "{\"category\":\"%s\",\"fcstDate\":\"%s\",\"fcstTime\":\"%s\",\"fcstValue\":\"%s\"}",
                category, fcstDate, fcstTime, value);
    }

    // ── collectWeather ───────────────────────────────────────────────────

    @Test
    void 종료된_페스티벌이면_API_호출없이_false() {
        Festival f = festival(1L, today().minusDays(10), today().minusDays(1));

        boolean result = weatherService.collectWeather(f);

        assertThat(result).isFalse();
        verify(restTemplate, never()).getForObject(any(URI.class), any());
    }

    @Test
    void serviceKey_미설정이면_false() {
        ReflectionTestUtils.setField(weatherService, "serviceKey", "");
        Festival f = festival(1L, today(), today().plusDays(1));

        assertThat(weatherService.collectWeather(f)).isFalse();
        verify(restTemplate, never()).getForObject(any(URI.class), any());
    }

    @Test
    void 시작일이_없으면_false() {
        Festival f = festival(1L, null, null);

        assertThat(weatherService.collectWeather(f)).isFalse();
    }

    @Test
    void 목표일이_조회가능범위를_벗어나면_false() {
        Festival f = festival(1L, today().plusDays(10), today().plusDays(15));

        assertThat(weatherService.collectWeather(f)).isFalse();
        verify(restTemplate, never()).getForObject(any(URI.class), any());
    }

    @Test
    void API_응답이_null이면_예외전파() {
        Festival f = festival(1L, today(), today().plusDays(1));
        given(restTemplate.getForObject(any(URI.class), eq(JsonNode.class))).willReturn(null);

        assertThatThrownBy(() -> weatherService.collectWeather(f))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("비어 있습니다");
    }

    @Test
    void API_resultCode가_실패면_예외전파() throws Exception {
        Festival f = festival(1L, today(), today().plusDays(1));
        JsonNode body = MAPPER.readTree("{\"response\":{\"header\":{\"resultCode\":\"03\"}}}");
        given(restTemplate.getForObject(any(URI.class), eq(JsonNode.class))).willReturn(body);

        assertThatThrownBy(() -> weatherService.collectWeather(f))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("기상청 API 오류");
    }

    @Test
    void 정상_수집시_TMN_TMX_최대강수확률_정오하늘_최대PTY_반영() throws Exception {
        Festival f = festival(1L, today(), today().plusDays(1));
        String date = today().format(KMA_DATE);
        String itemsJson = "[" + String.join(",",
                item("TMN", date, "0600", "10.0"),
                item("TMX", date, "1500", "25.0"),
                item("POP", date, "0900", "30"),
                item("POP", date, "1500", "70"),
                item("SKY", date, "0900", "1"),
                item("SKY", date, "1200", "3"),
                item("PTY", date, "0900", "0"),
                item("PTY", date, "1500", "1")
        ) + "]";
        given(restTemplate.getForObject(any(URI.class), eq(JsonNode.class))).willReturn(successBody(itemsJson));
        given(weatherRepository.findByFestivalId(1L)).willReturn(Optional.empty());

        boolean result = weatherService.collectWeather(f);

        assertThat(result).isTrue();
        ArgumentCaptor<FestivalWeather> captor = ArgumentCaptor.forClass(FestivalWeather.class);
        verify(weatherRepository).save(captor.capture());
        FestivalWeather saved = captor.getValue();
        assertThat(saved.getMinTemp()).isEqualTo(10.0);
        assertThat(saved.getMaxTemp()).isEqualTo(25.0);
        assertThat(saved.getRainProb()).isEqualTo(70);
        assertThat(saved.getSkyCode()).isEqualTo(SkyCode.CLOUDY);
        assertThat(saved.getPtyCode()).isEqualTo(PtyCode.RAIN);
    }

    @Test
    void TMN_TMX_없으면_TMP_기반_최저_최고기온_대체() throws Exception {
        Festival f = festival(1L, today(), today().plusDays(1));
        String date = today().format(KMA_DATE);
        String itemsJson = "[" + String.join(",",
                item("TMP", date, "0300", "12.0"),
                item("TMP", date, "1500", "22.0"),
                item("TMP", date, "0900", "18.0")
        ) + "]";
        given(restTemplate.getForObject(any(URI.class), eq(JsonNode.class))).willReturn(successBody(itemsJson));
        given(weatherRepository.findByFestivalId(1L)).willReturn(Optional.empty());

        weatherService.collectWeather(f);

        ArgumentCaptor<FestivalWeather> captor = ArgumentCaptor.forClass(FestivalWeather.class);
        verify(weatherRepository).save(captor.capture());
        assertThat(captor.getValue().getMinTemp()).isEqualTo(12.0);
        assertThat(captor.getValue().getMaxTemp()).isEqualTo(22.0);
    }

    @Test
    void 기존_날씨데이터가_있으면_갱신() throws Exception {
        Festival f = festival(1L, today(), today().plusDays(1));
        String date = today().format(KMA_DATE);
        String itemsJson = "[" + item("TMN", date, "0600", "5.0") + "," + item("TMX", date, "1500", "15.0") + "]";
        given(restTemplate.getForObject(any(URI.class), eq(JsonNode.class))).willReturn(successBody(itemsJson));
        FestivalWeather existing = FestivalWeather.of(f, new WeatherDto(date, 0, 0, 0, SkyCode.SUNNY, PtyCode.NONE));
        given(weatherRepository.findByFestivalId(1L)).willReturn(Optional.of(existing));

        weatherService.collectWeather(f);

        verify(weatherRepository).save(existing);
        assertThat(existing.getMinTemp()).isEqualTo(5.0);
    }

    // ── getByFestivalId ──────────────────────────────────────────────────

    @Test
    void 존재하지_않는_페스티벌_조회시_예외() {
        given(festivalRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> weatherService.getByFestivalId(1L)).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void 종료된_페스티벌은_캐시된_날씨만_반환() {
        Festival f = festival(1L, today().minusDays(10), today().minusDays(1));
        given(festivalRepository.findById(1L)).willReturn(Optional.of(f));
        FestivalWeather cached = FestivalWeather.of(f, new WeatherDto("20260101", 1, 2, 3, SkyCode.SUNNY, PtyCode.NONE));
        given(weatherRepository.findByFestivalId(1L)).willReturn(Optional.of(cached));

        Optional<WeatherDto> result = weatherService.getByFestivalId(1L);

        assertThat(result).isPresent();
        verify(restTemplate, never()).getForObject(any(URI.class), any());
    }

    @Test
    void 진행중_페스티벌은_수집시도후_캐시반환() throws Exception {
        Festival f = festival(1L, today(), today().plusDays(1));
        given(festivalRepository.findById(1L)).willReturn(Optional.of(f));
        String date = today().format(KMA_DATE);
        String itemsJson = "[" + item("TMN", date, "0600", "5.0") + "," + item("TMX", date, "1500", "15.0") + "]";
        given(restTemplate.getForObject(any(URI.class), eq(JsonNode.class))).willReturn(successBody(itemsJson));
        given(weatherRepository.findByFestivalId(1L)).willReturn(Optional.empty(),
                Optional.of(FestivalWeather.of(f, new WeatherDto(date, 5, 15, 0, SkyCode.SUNNY, PtyCode.NONE))));

        Optional<WeatherDto> result = weatherService.getByFestivalId(1L);

        assertThat(result).isPresent();
        verify(weatherRepository).save(any(FestivalWeather.class));
    }

    @Test
    void 수집중_API응답없어도_로그만_남기고_캐시반환() {
        Festival f = festival(1L, today(), today().plusDays(1));
        given(festivalRepository.findById(1L)).willReturn(Optional.of(f));
        given(restTemplate.getForObject(any(URI.class), eq(JsonNode.class))).willReturn(null);
        given(weatherRepository.findByFestivalId(1L)).willReturn(Optional.empty());

        Optional<WeatherDto> result = weatherService.getByFestivalId(1L);

        assertThat(result).isEmpty();
    }

    // ── removeAllByFestival ──────────────────────────────────────────────

    @Test
    void 페스티벌_삭제시_날씨데이터_일괄삭제() {
        weatherService.removeAllByFestival(1L);

        verify(weatherRepository).deleteByFestivalId(1L);
    }
}
