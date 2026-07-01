package com.feple.feple_backend.admin.ocr;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "gemini_daily_usage")
@Getter
@NoArgsConstructor
public class GeminiDailyUsage {

    @Id
    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private int count;

    public static GeminiDailyUsage of(LocalDate date) {
        GeminiDailyUsage u = new GeminiDailyUsage();
        u.date = date;
        u.count = 0;
        return u;
    }

    public void increment() {
        this.count++;
    }
}
