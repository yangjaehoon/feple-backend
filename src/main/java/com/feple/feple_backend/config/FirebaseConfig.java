package com.feple.feple_backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${app.firebase.credentials:}")
    private String credentialsJson;

    @PostConstruct
    public void initialize() {
        if (!StringUtils.hasText(credentialsJson)) {
            log.warn("[Firebase] 자격증명이 설정되지 않아 Firebase를 초기화하지 않습니다.");
            return;
        }
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }
        try {
            // YAML 파싱 또는 시크릿 주입 과정에서 추가된 앞뒤 공백·따옴표·BOM 제거
            String json = credentialsJson.trim();
            if (json.startsWith("\uFEFF")) json = json.substring(1); // UTF-8 BOM
            if (json.startsWith("'") && json.endsWith("'")) json = json.substring(1, json.length() - 1);
            if (json.startsWith("\"") && json.endsWith("\"")) json = json.substring(1, json.length() - 1);

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(
                            new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))))
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("[Firebase] 초기화 완료 (project: feple-31f3f)");
        } catch (Exception e) {
            log.error("[Firebase] 초기화 실패: {}", e.getMessage());
        }
    }
}
