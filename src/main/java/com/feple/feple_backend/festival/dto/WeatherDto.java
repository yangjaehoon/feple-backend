package com.feple.feple_backend.festival.dto;

public record WeatherDto(
        String fcstDate,    // YYYYMMDD
        double minTemp,     // 일 최저기온
        double maxTemp,     // 일 최고기온
        int rainProb,       // 강수확률 최댓값 (%)
        String skyCode,     // 1=맑음 3=구름많음 4=흐림
        String ptyCode      // 0=없음 1=비 2=비/눈 3=눈 4=소나기
) {}
