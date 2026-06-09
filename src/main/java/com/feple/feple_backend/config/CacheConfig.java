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

        // 관리자 사이드바 뱃지 카운트: 페이지 이동마다 4+ COUNT 쿼리 방지 — 30초 TTL
        CaffeineCache adminSidebarCountsCache = new CaffeineCache("adminSidebarCounts",
                Caffeine.newBuilder()
                        .expireAfterWrite(30, TimeUnit.SECONDS)
                        .maximumSize(1)
                        .build());

        // 통계 페이지 활동 지표: 6개 COUNT 쿼리 — 5분 TTL
        CaffeineCache adminActivityStatsCache = new CaffeineCache("adminActivityStats",
                Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(1)
                        .build());

        // 통계 페이지 콘텐츠 트렌드: 5개 집계 쿼리 — 10분 TTL
        CaffeineCache adminContentTrendCache = new CaffeineCache("adminContentTrend",
                Caffeine.newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(1)
                        .build());

        // 아티스트 이름순 전체 목록: 관리자 폼/드롭다운 4곳에서 매 요청마다 풀스캔 — 5분 TTL
        CaffeineCache allArtistsSortedByNameCache = new CaffeineCache("allArtistsSortedByName",
                Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(1)
                        .build());

        // 페스티벌 전체 목록(체크리스트/드롭다운): getAllFestivalsForAdmin() 풀스캔 — 5분 TTL
        CaffeineCache allFestivalsForAdminCache = new CaffeineCache("allFestivalsForAdmin",
                Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(1)
                        .build());

        // 통계 범위 쿼리(날짜별 4개 집계): date-range key 캐싱 — 10분 TTL
        CaffeineCache adminRangeStatsCache = new CaffeineCache("adminRangeStats",
                Caffeine.newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(100)
                        .build());

        // 대시보드 집계(총 유저/게시글 수, 최근 유저, 일별 통계): 5분 TTL
        CaffeineCache adminDashboardStatsCache = new CaffeineCache("adminDashboardStats",
                Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(20)
                        .build());

        // 대시보드 보류 항목(미승인 인증/신고/노래요청 개수·목록): 2분 TTL
        CaffeineCache adminPendingCountsCache = new CaffeineCache("adminPendingCounts",
                Caffeine.newBuilder()
                        .expireAfterWrite(2, TimeUnit.MINUTES)
                        .maximumSize(20)
                        .build());

        // 신고 페이지 타입별 카운트(pending+total × 3종): 30초 TTL
        CaffeineCache adminReportTypeCountsCache = new CaffeineCache("adminReportTypeCounts",
                Caffeine.newBuilder()
                        .expireAfterWrite(30, TimeUnit.SECONDS)
                        .maximumSize(10)
                        .build());

        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(weatherCache, hotPostsCache, artistRankingCache, topArtistsCache,
                adminSidebarCountsCache, adminActivityStatsCache, adminContentTrendCache,
                allArtistsSortedByNameCache, allFestivalsForAdminCache, adminRangeStatsCache,
                adminDashboardStatsCache, adminPendingCountsCache, adminReportTypeCountsCache));
        return manager;
    }
}
