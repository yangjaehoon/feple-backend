package com.feple.feple_backend.festival.entity;

import com.feple.feple_backend.festival.dto.WeatherDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class FestivalWeather {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long festivalId;

    private String fcstDate;
    private Double minTemp;
    private Double maxTemp;
    private Integer rainProb;
    private String skyCode;
    private String ptyCode;
    private LocalDateTime savedAt;

    public static FestivalWeather of(Long festivalId, WeatherDto dto) {
        FestivalWeather w = new FestivalWeather();
        w.festivalId = festivalId;
        w.apply(dto);
        return w;
    }

    public void apply(WeatherDto dto) {
        this.fcstDate = dto.fcstDate();
        this.minTemp = dto.minTemp();
        this.maxTemp = dto.maxTemp();
        this.rainProb = dto.rainProb();
        this.skyCode = dto.skyCode();
        this.ptyCode = dto.ptyCode();
        this.savedAt = LocalDateTime.now();
    }

    public WeatherDto toDto() {
        return new WeatherDto(fcstDate, minTemp, maxTemp, rainProb, skyCode, ptyCode);
    }
}
