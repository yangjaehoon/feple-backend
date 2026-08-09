package com.feple.feple_backend.festival.service;

import com.feple.feple_backend.festival.dto.WeatherDto;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.entity.FestivalWeather;
import com.feple.feple_backend.festival.repository.FestivalWeatherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// WeatherService.saveOrUpdate()를 별도 빈으로 분리 — 같은 클래스 안에서 private 메서드로 두면
// self-invocation이라 @Transactional을 붙여도 Spring AOP 프록시가 가로채지 못해 트랜잭션이
// 걸리지 않는다. 조회+저장을 실제로 하나의 트랜잭션으로 묶기 위해 별도 컴포넌트로 둔다.
@Component
@RequiredArgsConstructor
class FestivalWeatherStore {

    private final FestivalWeatherRepository weatherRepository;

    @Transactional
    public void saveOrUpdate(Festival festival, WeatherDto dto) {
        FestivalWeather weather = weatherRepository.findByFestivalId(festival.getId())
                .orElse(FestivalWeather.of(festival, dto));
        weather.apply(dto);
        weatherRepository.save(weather);
    }
}
