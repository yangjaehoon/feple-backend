package com.feple.feple_backend.festival.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** 기상청(KMA) 단기예보 API 인증키·베이스 URL. */
@ConfigurationProperties(prefix = "kma")
public record WeatherProperties(
        @DefaultValue("") String serviceKey,
        String baseUrl) {
}
