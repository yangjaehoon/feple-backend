package com.feple.feple_backend.admin.ocr;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

public interface GeminiDailyUsageRepository extends JpaRepository<GeminiDailyUsage, LocalDate> {

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO gemini_daily_usage (date, count) VALUES (:date, 1) " +
                   "ON DUPLICATE KEY UPDATE count = count + 1", nativeQuery = true)
    void upsertIncrement(@Param("date") LocalDate date);
}
