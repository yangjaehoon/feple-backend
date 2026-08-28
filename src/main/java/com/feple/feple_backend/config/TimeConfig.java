package com.feple.feple_backend.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 주입 가능한 시스템 시계.
 *
 * <p>토큰 만료 등 시간에 의존하는 로직이 {@code new Date()}·{@code Instant.now()}를 직접 부르면
 * 테스트에서 시간을 제어할 수 없어 음수 만료값 같은 편법을 쓰게 된다. 이 빈을 주입받아
 * {@code clock.instant()}로 현재 시각을 읽으면 테스트에서 {@link Clock#fixed}로 고정할 수 있다.
 */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
