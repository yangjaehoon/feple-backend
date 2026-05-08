package com.feple.feple_backend.search.service;

import com.feple.feple_backend.artist.service.ArtistService;
import com.feple.feple_backend.festival.service.FestivalService;
import com.feple.feple_backend.post.service.PostService;
import com.feple.feple_backend.search.dto.SearchResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final ArtistService artistService;
    private final FestivalService festivalService;
    private final PostService postService;

    private static final int MAX_RESULTS = 10;

    @Transactional(readOnly = true)
    public SearchResultDto search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return new SearchResultDto(List.of(), List.of(), List.of());
        }
        String kw = keyword.trim();
        return new SearchResultDto(
                artistService.searchArtists(kw).stream().limit(MAX_RESULTS).toList(),
                festivalService.searchFestivals(kw),
                postService.searchPosts(kw)
        );
    }
}
