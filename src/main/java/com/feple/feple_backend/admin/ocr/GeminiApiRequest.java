package com.feple.feple_backend.admin.ocr;

import java.time.Duration;
import java.util.Map;

record GeminiApiRequest(String url, String apiKey, Map<String, Object> body, Duration timeout) {
}
