package com.feple.feple_backend.admin.ocr;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Gemini API 키·일일 쿼터·OCR/URL-context 호출 파라미터.
 * OCR과 URL-context가 같은 {@code app.gemini} 접두사를 공유하므로 한 레코드로 묶는다.
 */
@ConfigurationProperties(prefix = "app.gemini")
public record GeminiProperties(
        @DefaultValue("") String apiKey,
        @DefaultValue("500") int dailyLimit,
        @DefaultValue("16384") int ocrMaxOutputTokens,
        @DefaultValue("90") int ocrTimeoutSeconds,
        @DefaultValue("60") int urlContextTimeoutSeconds,
        @DefaultValue("512") int urlContextMaxOutputTokens) {
}
