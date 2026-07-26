package com.feple.feple_backend.global.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Caching(evict = {
    @CacheEvict(value = "artistRanking",          allEntries = true),
    @CacheEvict(value = "topArtists",             allEntries = true),
    @CacheEvict(value = "allArtistsSortedByName", allEntries = true)
})
public @interface EvictArtistCaches {}
