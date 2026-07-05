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

    // 웹 스크래핑 전용 HttpClient — SsrfSafeDnsResolver로 모든 연결(리다이렉트 포함)의
    // DNS 조회를 검증해 TOCTOU 없이 SSRF를 차단한다.
    @Bean(destroyMethod = "close")
    public CloseableHttpClient safeScraperHttpClient() {
        var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDnsResolver(new SsrfSafeDnsResolver())
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.ofSeconds(15))
                        .build())
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setResponseTimeout(Timeout.ofSeconds(15))
                .setMaxRedirects(5)
                .build();

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }
}
