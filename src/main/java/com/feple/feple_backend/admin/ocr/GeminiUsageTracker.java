package com.feple.feple_backend.admin.ocr;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class GeminiUsageTracker {

    private static final ZoneId PACIFIC = ZoneId.of("America/Los_Angeles");

    @Value("${app.gemini.daily-limit:500}")
    private int dailyLimit;

    private final AtomicInteger todayCount = new AtomicInteger(0);
    private volatile LocalDate trackingDate = LocalDate.now(PACIFIC);

    public synchronized void increment() {
        resetIfNewDay();
        todayCount.incrementAndGet();
    }

    public synchronized int getTodayCount() {
        resetIfNewDay();
        return todayCount.get();
    }

    public int getDailyLimit() {
        return dailyLimit;
    }

    // 호출자가 이미 synchronized 메서드 안에 있을 때만 사용
    private void resetIfNewDay() {
        LocalDate today = LocalDate.now(PACIFIC);
        if (!today.equals(trackingDate)) {
            trackingDate = today;
            todayCount.set(0);
        }
    }
}
