package com.feple.feple_backend.festival.dto;

import com.feple.feple_backend.festival.entity.PtyCode;
import com.feple.feple_backend.festival.entity.SkyCode;

public record WeatherDto(
        String fcstDate,    // YYYYMMDD
        double minTemp,     // 일 최저기온
        double maxTemp,     // 일 최고기온
        int rainProb,       // 강수확률 최댓값 (%)
        SkyCode skyCode,
        PtyCode ptyCode
) {}
