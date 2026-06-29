package com.feple.feple_backend.festival.entity;

import com.feple.feple_backend.festival.dto.WeatherDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Getter
@NoArgsConstructor
public class FestivalWeather {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "festival_id", nullable = false, unique = true)
    private Festival festival;

    @Column(name = "fcst_date", columnDefinition = "DATE")
    private LocalDate fcstDate;

    private static final DateTimeFormatter KMA_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private Double minTemp;
    private Double maxTemp;
    private Integer rainProb;
    private String skyCode;
    private String ptyCode;
    private LocalDateTime savedAt;

    public Long getFestivalId() { return festival.getId(); }

    public static FestivalWeather of(Festival festival, WeatherDto dto) {
        FestivalWeather weather = new FestivalWeather();
        weather.festival = festival;
        weather.apply(dto);
        return weather;
    }

    public void apply(WeatherDto dto) {
        this.fcstDate = dto.fcstDate() != null ? LocalDate.parse(dto.fcstDate(), KMA_DATE) : null;
        this.minTemp = dto.minTemp();
        this.maxTemp = dto.maxTemp();
        this.rainProb = dto.rainProb();
        this.skyCode = dto.skyCode();
        this.ptyCode = dto.ptyCode();
        this.savedAt = LocalDateTime.now();
    }

    public WeatherDto toDto() {
        return new WeatherDto(
                fcstDate != null ? fcstDate.format(KMA_DATE) : null,
                minTemp, maxTemp, rainProb, skyCode, ptyCode);
    }
}
