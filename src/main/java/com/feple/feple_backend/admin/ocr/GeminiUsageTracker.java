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

    private static final ZoneId PACIFIC = ZoneId.of("America/Los_Angeles");

    @Value("${app.gemini.daily-limit:500}")
    private int dailyLimit;

    private final GeminiDailyUsageRepository repository;

    @Transactional
    public synchronized void increment() {
        LocalDate today = LocalDate.now(PACIFIC);
        GeminiDailyUsage usage = repository.findById(today)
                .orElse(GeminiDailyUsage.of(today));
        usage.increment();
        repository.save(usage);
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
