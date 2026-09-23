package com.feple.feple_backend.festival.repository;

import com.feple.feple_backend.festival.entity.FestivalWeather;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FestivalWeatherRepository extends JpaRepository<FestivalWeather, Long> {

    @Query("SELECT fw FROM FestivalWeather fw WHERE fw.festival.id = :festivalId")
    Optional<FestivalWeather> findByFestivalId(@Param("festivalId") Long festivalId);
}
