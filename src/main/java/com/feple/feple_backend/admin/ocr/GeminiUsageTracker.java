package com.feple.feple_backend.admin.ocr;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class GeminiUsageTracker {

    // Gemini API 일일 쿼터는 태평양 시간 자정 기준으로 리셋됨 (KST 아님)
    private static final ZoneId PACIFIC = ZoneId.of("America/Los_Angeles");

    @Value("${app.gemini.daily-limit:500}")
    private int dailyLimit;

    private final GeminiDailyUsageRepository repository;

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
        return dailyLimit;
    }
}
