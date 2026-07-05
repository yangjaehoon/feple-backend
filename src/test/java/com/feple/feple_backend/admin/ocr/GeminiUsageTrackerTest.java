package com.feple.feple_backend.admin.ocr;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class GeminiUsageTrackerTest {

    @Mock GeminiDailyUsageRepository repository;

    @InjectMocks GeminiUsageTracker tracker;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tracker, "dailyLimit", 500);
    }

    @Test
    void increment_오늘_태평양_날짜로_upsertIncrement_호출() {
        LocalDate today = LocalDate.now(ZoneId.of("America/Los_Angeles"));

        tracker.increment();

        then(repository).should().upsertIncrement(today);
    }

    @Test
    void getTodayCount_레코드_있으면_count_반환() {
        LocalDate today = LocalDate.now(ZoneId.of("America/Los_Angeles"));
        GeminiDailyUsage usage = GeminiDailyUsage.of(today);
        usage.increment();
        given(repository.findById(today)).willReturn(Optional.of(usage));

        int count = tracker.getTodayCount();

        assertThat(count).isEqualTo(1);
    }

    @Test
    void getTodayCount_레코드_없으면_0_반환() {
        given(repository.findById(any())).willReturn(Optional.empty());

        int count = tracker.getTodayCount();

        assertThat(count).isZero();
    }

    @Test
    void getDailyLimit_설정값_반환() {
        assertThat(tracker.getDailyLimit()).isEqualTo(500);
    }
}
