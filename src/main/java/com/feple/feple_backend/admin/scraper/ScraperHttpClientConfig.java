package com.feple.feple_backend.admin.scraper;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ScraperHttpClientConfig {

    private static final int CONNECT_TIMEOUT_SECONDS = 15;
    private static final int RESPONSE_TIMEOUT_SECONDS = 15;
    private static final int MAX_REDIRECTS = 5;

    // 웹 스크래핑 전용 HttpClient — SsrfSafeDnsResolver로 모든 연결(리다이렉트 포함)의
    // DNS 조회를 검증해 TOCTOU 없이 SSRF를 차단한다.
    @Bean(destroyMethod = "close")
    public CloseableHttpClient safeScraperHttpClient() {
        var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDnsResolver(new SsrfSafeDnsResolver())
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.ofSeconds(CONNECT_TIMEOUT_SECONDS))
                        .build())
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setResponseTimeout(Timeout.ofSeconds(RESPONSE_TIMEOUT_SECONDS))
                .setMaxRedirects(MAX_REDIRECTS)
                .build();

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }
}
