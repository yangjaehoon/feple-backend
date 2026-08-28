package com.feple.feple_backend.admin.festival;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** 관리자 페스티벌 상세 페이지 지도 렌더링용 Google Maps JS API 키. 미설정이면 지도가 표시되지 않는다. */
@ConfigurationProperties(prefix = "app.google.maps")
public record GoogleMapsProperties(@DefaultValue("") String key) {
}
