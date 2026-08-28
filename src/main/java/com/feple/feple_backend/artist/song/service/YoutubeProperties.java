package com.feple.feple_backend.artist.song.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** YouTube Data API 키. 미설정이면 셋리스트 영상 검색이 빈 결과를 반환한다. */
@ConfigurationProperties(prefix = "app.youtube")
public record YoutubeProperties(@DefaultValue("") String apiKey) {
}
