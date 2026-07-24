package com.feple.feple_backend.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    // 카카오 사용자 정보 조회 — 단순 API라 짧은 타임아웃으로 충분
    @Bean
    public WebClient kakaoWebClient() {
        return WebClient.builder().clientConnector(connector(5, 10)).build();
    }

    // Gemini OCR/URL 컨텍스트 — 호출부에서 최대 90초까지 block()하므로
    // WebClient 자체 타임아웃은 그보다 넉넉하게 잡아야 block()이 먼저 작동한다.
    @Bean
    public WebClient geminiWebClient() {
        return WebClient.builder().clientConnector(connector(10, 100)).build();
    }

    private ReactorClientHttpConnector connector(int connectTimeoutSec, int responseTimeoutSec) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutSec * 1000)
                .responseTimeout(Duration.ofSeconds(responseTimeoutSec))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(responseTimeoutSec, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(responseTimeoutSec, TimeUnit.SECONDS)));
        return new ReactorClientHttpConnector(httpClient);
    }
}
