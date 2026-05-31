package com.feple.feple_backend.festival.repository;

import com.feple.feple_backend.festival.entity.FestivalWeather;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FestivalWeatherRepository extends JpaRepository<FestivalWeather, Long> {
    Optional<FestivalWeather> findByFestivalId(Long festivalId);
}
