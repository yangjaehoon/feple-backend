package com.feple.feple_backend.file;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** CDN 베이스 URL. 미설정({@code ""})이면 S3 버킷 URL을 직접 사용한다. */
@ConfigurationProperties(prefix = "app.cdn")
public record CdnProperties(@DefaultValue("") String baseUrl) {
}
