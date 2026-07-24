package com.feple.feple_backend.global.cache;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// TTL 설정: CacheConfig — adminPendingCounts=2분, adminSidebarCounts=30초, adminReportTypeCounts=30초
// 사용처: PostReportAdminServiceImpl, CommentReportAdminServiceImpl, AdminPendingItemsServiceImpl
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Caching(evict = {
    @CacheEvict(value = "adminPendingCounts",    allEntries = true),
    @CacheEvict(value = "adminSidebarCounts",    allEntries = true),
    @CacheEvict(value = "adminReportTypeCounts", allEntries = true)
})
public @interface EvictAdminReportCaches {}
