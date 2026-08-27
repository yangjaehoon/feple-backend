package com.feple.feple_backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class AsyncConfigTest {

    private final AsyncConfig config = new AsyncConfig();

    @Test
    void 비동기_실행_스레드에_호출측_MDC_컨텍스트가_복사된다() throws InterruptedException {
        MDC.put("requestId", "abc123");
        try {
            Executor executor = config.getAsyncExecutor();
            assertThat(executor).isNotNull();
            AtomicReference<String> captured = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);

            executor.execute(() -> {
                captured.set(MDC.get("requestId"));
                latch.countDown();
            });

            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(captured.get()).isEqualTo("abc123");
        } finally {
            MDC.clear();
        }
    }

    @Test
    void 대시보드_실행기도_MDC_컨텍스트가_복사된다() throws InterruptedException {
        MDC.put("requestId", "dash456");
        try {
            Executor executor = config.dashboardExecutor();
            assertThat(executor).isNotNull();
            AtomicReference<String> captured = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);

            executor.execute(() -> {
                captured.set(MDC.get("requestId"));
                latch.countDown();
            });

            assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(captured.get()).isEqualTo("dash456");
        } finally {
            MDC.clear();
        }
    }
}
