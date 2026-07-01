package com.feple.feple_backend.admin.ocr;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface GeminiDailyUsageRepository extends JpaRepository<GeminiDailyUsage, LocalDate> {
}
