package com.feple.feple_backend.festival.dto;

public record AirQualityDto(
        String stationName,
        String sidoName,
        String pm10Value,
        String pm25Value,
        String pm10Grade,
        String pm25Grade,
        String khaiValue,
        String khaiGrade,
        String dataTime
) {}
