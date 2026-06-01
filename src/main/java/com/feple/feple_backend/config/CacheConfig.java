package com.feple.feple_backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public CacheManager cacheManager() {
        CaffeineCache weatherCache = new CaffeineCache("weather",
                Caffeine.newBuilder()
                        .expireAfterWrite(1, TimeUnit.HOURS)
                        .maximumSize(50)
                        .build());

        // 핫 게시글: 7일치 DB 풀스캔 방지 — 10분 TTL (좋아요 반영 지연 허용)
        CaffeineCache hotPostsCache = new CaffeineCache("hotPosts",
                Caffeine.newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(1)
                        .build());

        // 아티스트 랭킹(weeklyScore 정렬): 스케줄러(매주 월) 후 명시적 eviction + 24h TTL 보조
        CaffeineCache artistRankingCache = new CaffeineCache("artistRanking",
                Caffeine.newBuilder()
                        .expireAfterWrite(24, TimeUnit.HOURS)
                        .maximumSize(1)
                        .build());

        // 인기 아티스트(followerCount 정렬): limit 파라미터를 키로 — 1시간 TTL
        CaffeineCache topArtistsCache = new CaffeineCache("topArtists",
                Caffeine.newBuilder()
                        .expireAfterWrite(1, TimeUnit.HOURS)
                        .maximumSize(20)
                        .build());

        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(weatherCache, hotPostsCache, artistRankingCache, topArtistsCache));
        return manager;
    }
}
