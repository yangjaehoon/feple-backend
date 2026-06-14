package com.feple.feple_backend.admin;

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

    public void increment() {
        resetIfNewDay();
        todayCount.incrementAndGet();
    }

    public int getTodayCount() {
        resetIfNewDay();
        return todayCount.get();
    }

    public int getDailyLimit() {
        return dailyLimit;
    }

    private synchronized void resetIfNewDay() {
        LocalDate today = LocalDate.now(PACIFIC);
        if (!today.equals(trackingDate)) {
            trackingDate = today;
            todayCount.set(0);
        }
    }
}
