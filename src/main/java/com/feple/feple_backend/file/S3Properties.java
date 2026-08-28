package com.feple.feple_backend.file;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * S3 버킷·presigned URL 설정. 값을 읽는 서비스가 생성자로 주입받아
 * 테스트에서 이 레코드를 그대로 {@code new} 해서 넘길 수 있게 한다({@code @Value} 필드 + 리플렉션 제거).
 */
@ConfigurationProperties(prefix = "app.s3")
public record S3Properties(
        String bucket,
        @DefaultValue("10") long presignMinutes,
        // GET presigned URL TTL — 7일(최대값). CachedNetworkImage가 URL을 키로 캐시하므로
        // TTL이 짧으면 만료 후 재시도 시 403 발생.
        @DefaultValue("168") long getPresignHours) {
}
