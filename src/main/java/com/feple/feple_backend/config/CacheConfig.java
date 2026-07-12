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
        CaffeineCache weatherCache = cache("weather", 1, TimeUnit.HOURS, 50);

        // 인기 게시글: 7일치 DB 풀스캔 방지 — 10분 TTL (좋아요 반영 지연 허용)
        CaffeineCache popularPostsCache = cache("popularPosts", 10, TimeUnit.MINUTES, 1);

        // 아티스트 랭킹(weeklyScore 정렬): 스케줄러(매주 월) 후 명시적 eviction + 24h TTL 보조
        CaffeineCache artistRankingCache = cache("artistRanking", 24, TimeUnit.HOURS, 1);

        // 인기 아티스트(followerCount 정렬): limit 파라미터를 키로 — 1시간 TTL
        CaffeineCache topArtistsCache = cache("topArtists", 1, TimeUnit.HOURS, 20);

        // 관리자 사이드바 뱃지 카운트: 페이지 이동마다 4+ COUNT 쿼리 방지 — 30초 TTL
        CaffeineCache adminSidebarCountsCache = cache("adminSidebarCounts", 30, TimeUnit.SECONDS, 1);

        // 통계 페이지 활동 지표: 6개 COUNT 쿼리 — 5분 TTL
        CaffeineCache adminActivityStatsCache = cache("adminActivityStats", 5, TimeUnit.MINUTES, 1);

        // 통계 페이지 콘텐츠 트렌드: 5개 집계 쿼리 — 10분 TTL
        CaffeineCache adminContentTrendCache = cache("adminContentTrend", 10, TimeUnit.MINUTES, 1);

        // 아티스트 이름순 전체 목록: 관리자 폼/드롭다운 4곳에서 매 요청마다 풀스캔 — 5분 TTL
        CaffeineCache allArtistsSortedByNameCache = cache("allArtistsSortedByName", 5, TimeUnit.MINUTES, 1);

        // 페스티벌 전체 목록(체크리스트/드롭다운): getAllFestivalsForAdmin() 풀스캔 — 5분 TTL
        CaffeineCache allFestivalsForAdminCache = cache("allFestivalsForAdmin", 5, TimeUnit.MINUTES, 1);

        // 통계 범위 쿼리(날짜별 4개 집계): date-range key 캐싱 — 10분 TTL
        CaffeineCache adminRangeStatsCache = cache("adminRangeStats", 10, TimeUnit.MINUTES, 100);

        // 대시보드 집계(총 유저/게시글 수, 최근 유저, 일별 통계): 5분 TTL
        CaffeineCache adminDashboardStatsCache = cache("adminDashboardStats", 5, TimeUnit.MINUTES, 20);

        // 대시보드 보류 항목(미승인 인증/신고/노래요청 개수·목록): 2분 TTL
        CaffeineCache adminPendingCountsCache = cache("adminPendingCounts", 2, TimeUnit.MINUTES, 20);

        // 신고 페이지 타입별 카운트(pending+total × 3종): 30초 TTL
        CaffeineCache adminReportTypeCountsCache = cache("adminReportTypeCounts", 30, TimeUnit.SECONDS, 10);

        // 진행 중 페스티벌 COUNT: 목록 페이지 매 로드마다 실행되던 쿼리 — 5분 TTL, 페스티벌 변경 시 evict
        CaffeineCache activeFestivalCountCache = cache("activeFestivalCount", 5, TimeUnit.MINUTES, 10);

        // 체크리스트 맵(festivalId → FestivalChecklist): 목록 페이지 매 로드마다 IN 쿼리 — 5분 TTL, toggle/saveMemo 시 evict
        CaffeineCache festivalChecklistMapCache = cache("festivalChecklistMap", 5, TimeUnit.MINUTES, 1);

        // 페스티벌 상세(id별): 매 요청마다 findById 방지 — 10분 TTL, update/delete 시 해당 키 evict
        CaffeineCache festivalDetailCache = cache("festivalDetail", 10, TimeUnit.MINUTES, 200);

        // 아티스트 상세(id별): 매 요청마다 findById 방지 — 10분 TTL, update/delete/사진변경 시 해당 키 evict
        CaffeineCache artistDetailCache = cache("artistDetail", 10, TimeUnit.MINUTES, 300);

        // 타임테이블(festivalId별): 관리자 수정 전까지 불변 — 30분 TTL, create/update/delete 시 해당 키 evict
        CaffeineCache timetableCache = cache("timetable", 30, TimeUnit.MINUTES, 100);

        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(weatherCache, popularPostsCache, artistRankingCache, topArtistsCache,
                adminSidebarCountsCache, adminActivityStatsCache, adminContentTrendCache,
                allArtistsSortedByNameCache, allFestivalsForAdminCache, adminRangeStatsCache,
                adminDashboardStatsCache, adminPendingCountsCache, adminReportTypeCountsCache,
                activeFestivalCountCache, festivalChecklistMapCache,
                festivalDetailCache, artistDetailCache, timetableCache));
        return manager;
    }

    private CaffeineCache cache(String name, long duration, TimeUnit unit, long maxSize) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .recordStats()
                .expireAfterWrite(duration, unit)
                .maximumSize(maxSize)
                .build());
    }
}
