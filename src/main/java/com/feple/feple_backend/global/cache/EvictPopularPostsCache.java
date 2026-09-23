package com.feple.feple_backend.global.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.cache.annotation.CacheEvict;

/**
 * 인기글 캐시(popularPosts)를 비운다 — 게시글의 삭제·복구·블라인드·블라인드 해제 경로에 붙인다.
 *
 * <p>이 캐시는 엔티티가 아니라 이미 직렬화된 PostResponseDto를 담고 있어 원본 상태 변화가 전혀
 * 반영되지 않는다. TTL(10분)만 있고 evict가 없으면, 관리자가 내리거나 신고 누적으로 자동
 * 블라인드된 인기글이 홈 화면 상단에 최대 10분 더 제목·본문과 함께 노출된다. TTL 주석이 근거로
 * 든 "좋아요 수 반영 지연 허용"과 달리 모더레이션 지연은 의도된 범위가 아니다.
 *
 * <p>캐시 엔트리가 1개뿐이라(CacheConfig: maxSize=1) allEntries 비용은 무시할 수 있다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@CacheEvict(value = "popularPosts", allEntries = true)
public @interface EvictPopularPostsCache {}
