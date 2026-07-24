package com.feple.feple_backend.global.cache;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Caching(evict = {
    @CacheEvict(value = "artistRanking",          allEntries = true),
    @CacheEvict(value = "topArtists",             allEntries = true),
    @CacheEvict(value = "allArtistsSortedByName", allEntries = true)
})
public @interface EvictArtistCaches {}
