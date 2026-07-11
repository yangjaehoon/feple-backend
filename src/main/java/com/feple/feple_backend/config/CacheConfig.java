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
                        .recordStats()
                        .expireAfterWrite(1, TimeUnit.HOURS)
                        .maximumSize(50)
                        .build());

        // 인기 게시글: 7일치 DB 풀스캔 방지 — 10분 TTL (좋아요 반영 지연 허용)
        CaffeineCache popularPostsCache = new CaffeineCache("popularPosts",
                Caffeine.newBuilder()
                        .recordStats()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(1)
                        .build());

        // 아티스트 랭킹(weeklyScore 정렬): 스케줄러(매주 월) 후 명시적 eviction + 24h TTL 보조
        CaffeineCache artistRankingCache = new CaffeineCache("artistRanking",
                Caffeine.newBuilder()
                        .recordStats()
                        .expireAfterWrite(24, TimeUnit.HOURS)
                        .maximumSize(1)
                        .build());

        // 인기 아티스트(followerCount 정렬): limit 파라미터를 키로 — 1시간 TTL
        CaffeineCache topArtistsCache = new CaffeineCache("topArtists",
                Caffeine.newBuilder()
                        .recordStats()
                        .expireAfterWrite(1, TimeUnit.HOURS)
                        .maximumSize(20)
                        .build());

        // 관리자 사이드바 뱃지 카운트: 페이지 이동마다 4+ COUNT 쿼리 방지 — 30초 TTL
        CaffeineCache adminSidebarCountsCache = new CaffeineCache("adminSidebarCounts",
                Caffeine.newBuilder()
                        .recordStats()
                        .expireAfterWrite(30, TimeUnit.SECONDS)
                        .maximumSize(1)
                        .build());

        // 통계 페이지 활동 지표: 6개 COUNT 쿼리 — 5분 TTL
        CaffeineCache adminActivityStatsCache = new CaffeineCache("adminActivityStats",
                Caffeine.newBuilder()
                        .recordStats()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(1)
                        .build());

        // 통계 페이지 콘텐츠 트렌드: 5개 집계 쿼리 — 10분 TTL
        CaffeineCache adminContentTrendCache = new CaffeineCache("adminContentTrend",
                Caffeine.newBuilder()
                        .recordStats()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(1)
                        .build());

        // 아티스트 이름순 전체 목록: 관리자 폼/드롭다운 4곳에서 매 요청마다 풀스캔 — 5분 TTL
        CaffeineCache allArtistsSortedByNameCache = new CaffeineCache("allArtistsSortedByName",
                Caffeine.newBuilder()
                        .recordStats()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(1)
                        .build());

        // 페스티벌 전체 목록(체크리스트/드롭다운): getAllFestivalsForAdmin() 풀스캔 — 5분 TTL
        CaffeineCache allFestivalsForAdminCache = new CaffeineCache("allFestivalsForAdmin",
                Caffeine.newBuilder()
                        .recordStats()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(1)
                        .build());

        // 통계 범위 쿼리(날짜별 4개 집계): date-range key 캐싱 — 10분 TTL
        CaffeineCache adminRangeStatsCache = new CaffeineCache("adminRangeStats",
                Caffeine.newBuilder()
                        .recordStats()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(100)
                        .build());

        // 대시보드 집계(총 유저/게시글 수, 최근 유저, 일별 통계): 5분 TTL
        CaffeineCache adminDashboardStatsCache = new CaffeineCache("adminDashboardStats",
                Caffeine.newBuilder()
                        .recordStats()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(20)
                        .build());

        // 대시보드 보류 항목(미승인 인증/신고/노래요청 개수·목록): 2분 TTL
        CaffeineCache adminPendingCountsCache = new CaffeineCache("adminPendingCounts",
                Caffeine.newBuilder()
                        .recordStats()
                        .expireAfterWrite(2, TimeUnit.MINUTES)
                        .maximumSize(20)
                        .build());

        // 신고 페이지 타입별 카운트(pending+total × 3종): 30초 TTL
        CaffeineCache adminReportTypeCountsCache = new CaffeineCache("adminReportTypeCounts",
                Caffeine.newBuilder()
                        .recordStats()
                        .expireAfterWrite(30, TimeUnit.SECONDS)
                        .maximumSize(10)
                        .build());

        // 진행 중 페스티벌 COUNT: 목록 페이지 매 로드마다 실행되던 쿼리 — 5분 TTL, 페스티벌 변경 시 evict
        CaffeineCache activeFestivalCountCache = new CaffeineCache("activeFestivalCount",
                Caffeine.newBuilder()
                        .recordStats()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(10)
                        .build());

        // 체크리스트 맵(festivalId → FestivalChecklist): 목록 페이지 매 로드마다 IN 쿼리 — 5분 TTL, toggle/saveMemo 시 evict
        CaffeineCache festivalChecklistMapCache = new CaffeineCache("festivalChecklistMap",
                Caffeine.newBuilder()
                        .recordStats()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(1)
                        .build());

        // 페스티벌 상세(id별): 매 요청마다 findById 방지 — 10분 TTL, update/delete 시 해당 키 evict
        CaffeineCache festivalDetailCache = new CaffeineCache("festivalDetail",
                Caffeine.newBuilder()
                        .recordStats()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(200)
                        .build());

        // 아티스트 상세(id별): 매 요청마다 findById 방지 — 10분 TTL, update/delete/사진변경 시 해당 키 evict
        CaffeineCache artistDetailCache = new CaffeineCache("artistDetail",
                Caffeine.newBuilder()
                        .recordStats()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(300)
                        .build());

        // 타임테이블(festivalId별): 관리자 수정 전까지 불변 — 30분 TTL, create/update/delete 시 해당 키 evict
        CaffeineCache timetableCache = new CaffeineCache("timetable",
                Caffeine.newBuilder()
                        .recordStats()
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .maximumSize(100)
                        .build());

        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(weatherCache, popularPostsCache, artistRankingCache, topArtistsCache,
                adminSidebarCountsCache, adminActivityStatsCache, adminContentTrendCache,
                allArtistsSortedByNameCache, allFestivalsForAdminCache, adminRangeStatsCache,
                adminDashboardStatsCache, adminPendingCountsCache, adminReportTypeCountsCache,
                activeFestivalCountCache, festivalChecklistMapCache,
                festivalDetailCache, artistDetailCache, timetableCache));
        return manager;
    }
}
