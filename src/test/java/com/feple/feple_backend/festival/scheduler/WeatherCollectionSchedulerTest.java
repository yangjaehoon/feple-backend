package com.feple.feple_backend.festival.scheduler;

import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.festival.service.WeatherService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class WeatherCollectionSchedulerTest {

    @Mock FestivalRepository festivalRepository;
    @Mock WeatherService weatherService;

    @InjectMocks WeatherCollectionScheduler scheduler;

    private Festival festival(Long id) {
        return Festival.builder().id(id).title("페스티벌" + id).build();
    }

    @Test
    void 수집대상_없으면_날씨서비스_호출_안함() {
        given(festivalRepository.findOngoingOrStartingBefore(any(), any())).willReturn(List.of());

        scheduler.collect();

        then(weatherService).shouldHaveNoInteractions();
    }

    @Test
    void 수집대상_있으면_전부_수집_시도() {
        Festival f1 = festival(1L);
        Festival f2 = festival(2L);
        given(festivalRepository.findOngoingOrStartingBefore(any(), any())).willReturn(List.of(f1, f2));
        given(weatherService.collectWeather(f1)).willReturn(true);
        given(weatherService.collectWeather(f2)).willReturn(false);

        scheduler.collect();

        then(weatherService).should().collectWeather(f1);
        then(weatherService).should().collectWeather(f2);
    }

    @Test
    void 일부_수집_실패해도_나머지는_계속_처리() {
        Festival f1 = festival(1L);
        Festival f2 = festival(2L);
        given(festivalRepository.findOngoingOrStartingBefore(any(), any())).willReturn(List.of(f1, f2));
        willThrow(new RuntimeException("API 오류")).given(weatherService).collectWeather(f1);
        given(weatherService.collectWeather(f2)).willReturn(true);

        scheduler.collect();

        then(weatherService).should().collectWeather(f2);
    }
}
