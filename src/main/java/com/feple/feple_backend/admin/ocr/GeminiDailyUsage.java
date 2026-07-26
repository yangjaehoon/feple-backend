package com.feple.feple_backend.admin.ocr;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

}
