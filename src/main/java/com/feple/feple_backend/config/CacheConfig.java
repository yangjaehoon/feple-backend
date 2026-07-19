package com.feple.feple_backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
public class CacheConfig {

    private record CacheSpec(String name, Duration ttl, long maxSize) {}

    private static final List<CacheSpec> CACHE_SPECS = List.of(
            new CacheSpec("weather", Duration.ofHours(1), 50),

            // 인기 게시글: 7일치 DB 풀스캔 방지 — 10분 TTL (좋아요 반영 지연 허용)
            new CacheSpec("popularPosts", Duration.ofMinutes(10), 1),

            // 아티스트 랭킹(weeklyScore 정렬): 스케줄러(매주 월) 후 명시적 eviction + 24h TTL 보조
            new CacheSpec("artistRanking", Duration.ofHours(24), 1),

            // 인기 아티스트(followerCount 정렬): limit 파라미터를 키로 — 1시간 TTL
            new CacheSpec("topArtists", Duration.ofHours(1), 20),

            // 관리자 사이드바 뱃지 카운트: 페이지 이동마다 4+ COUNT 쿼리 방지 — 30초 TTL
            new CacheSpec("adminSidebarCounts", Duration.ofSeconds(30), 1),

            // 통계 페이지 활동 지표: 6개 COUNT 쿼리 — 5분 TTL
            new CacheSpec("adminActivityStats", Duration.ofMinutes(5), 1),

            // 통계 페이지 콘텐츠 트렌드: 5개 집계 쿼리 — 10분 TTL
            new CacheSpec("adminContentTrend", Duration.ofMinutes(10), 1),

            // 아티스트 이름순 전체 목록: 관리자 폼/드롭다운 4곳에서 매 요청마다 풀스캔 — 5분 TTL
            new CacheSpec("allArtistsSortedByName", Duration.ofMinutes(5), 1),

            // 페스티벌 전체 목록(체크리스트/드롭다운): getAllFestivalsForAdmin() 풀스캔 — 5분 TTL
            new CacheSpec("allFestivalsForAdmin", Duration.ofMinutes(5), 1),

            // 통계 범위 쿼리(날짜별 4개 집계): date-range key 캐싱 — 10분 TTL
            new CacheSpec("adminRangeStats", Duration.ofMinutes(10), 100),

            // 대시보드 집계(총 유저/게시글 수, 최근 유저, 일별 통계): 5분 TTL
            new CacheSpec("adminDashboardStats", Duration.ofMinutes(5), 20),

            // 대시보드 보류 항목(미승인 인증/신고/노래요청 개수·목록): 2분 TTL
            new CacheSpec("adminPendingCounts", Duration.ofMinutes(2), 20),

            // 신고 페이지 타입별 카운트(pending+total × 3종): 30초 TTL
            new CacheSpec("adminReportTypeCounts", Duration.ofSeconds(30), 10),

            // 진행 중 페스티벌 COUNT: 목록 페이지 매 로드마다 실행되던 쿼리 — 5분 TTL, 페스티벌 변경 시 evict
            new CacheSpec("activeFestivalCount", Duration.ofMinutes(5), 10),

            // 체크리스트 맵(festivalId → FestivalChecklist): 목록 페이지 매 로드마다 IN 쿼리 — 5분 TTL, toggle/saveMemo 시 evict
            new CacheSpec("festivalChecklistMap", Duration.ofMinutes(5), 1),

            // 페스티벌 상세(id별): 매 요청마다 findById 방지 — 10분 TTL, update/delete 시 해당 키 evict
            new CacheSpec("festivalDetail", Duration.ofMinutes(10), 200),

            // 아티스트 상세(id별): 매 요청마다 findById 방지 — 10분 TTL, update/delete/사진변경 시 해당 키 evict
            new CacheSpec("artistDetail", Duration.ofMinutes(10), 300),

            // 타임테이블(festivalId별): 관리자 수정 전까지 불변 — 30분 TTL, create/update/delete 시 해당 키 evict
            new CacheSpec("timetable", Duration.ofMinutes(30), 100)
    );

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public CacheManager cacheManager() {
        List<Cache> caches = CACHE_SPECS.stream().<Cache>map(this::toCaffeineCache).toList();
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(caches);
        return manager;
    }

    private CaffeineCache toCaffeineCache(CacheSpec spec) {
        return new CaffeineCache(spec.name(), Caffeine.newBuilder()
                .recordStats()
                .expireAfterWrite(spec.ttl())
                .maximumSize(spec.maxSize())
                .build());
    }
}
