package com.feple.feple_backend.admin.ocr;

import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GeminiUsageTracker {

    // Gemini API 일일 쿼터는 태평양 시간 자정 기준으로 리셋됨 (KST 아님)
    private static final ZoneId PACIFIC = ZoneId.of("America/Los_Angeles");

    private final GeminiDailyUsageRepository repository;
    private final GeminiProperties geminiProperties;

    @Transactional
    public void increment() {
        repository.upsertIncrement(LocalDate.now(PACIFIC));
    }

    @Transactional(readOnly = true)
    public int getTodayCount() {
        LocalDate today = LocalDate.now(PACIFIC);
        return repository.findById(today)
                .map(GeminiDailyUsage::getCount)
                .orElse(0);
    }

    public int getDailyLimit() {
        return geminiProperties.dailyLimit();
    }
}
