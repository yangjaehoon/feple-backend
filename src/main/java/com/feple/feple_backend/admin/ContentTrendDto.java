package com.feple.feple_backend.admin;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.post.entity.Post;

import java.util.List;

public record ContentTrendDto(
        List<TopKeywordDto> topKeywords,
        List<Festival> topFestivalsByLike,
        List<Festival> upcomingHotFestivals,
        List<Artist> topArtistsByFollower,
        List<Post> topPostsByLike
) {}
