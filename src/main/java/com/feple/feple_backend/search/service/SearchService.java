package com.feple.feple_backend.search.service;

import com.feple.feple_backend.artist.service.ArtistService;
import com.feple.feple_backend.festival.service.FestivalService;
import com.feple.feple_backend.post.service.PostService;
import com.feple.feple_backend.search.dto.SearchResultDto;
import com.feple.feple_backend.search.dto.SuggestionDto;
import com.feple.feple_backend.search.entity.SearchLog;
import com.feple.feple_backend.search.repository.SearchLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final ArtistService artistService;
    private final FestivalService festivalService;
    private final PostService postService;
    private final SearchLogRepository searchLogRepository;

    private static final int MAX_RESULTS = 10;
    private static final int MAX_SUGGESTIONS = 5;

    public SearchResultDto search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return new SearchResultDto(List.of(), List.of(), List.of());
        }
        String kw = keyword.trim();
        searchLogRepository.save(SearchLog.of(kw));
        return new SearchResultDto(
                artistService.searchArtists(kw).stream().limit(MAX_RESULTS).toList(),
                festivalService.searchFestivals(kw).stream().limit(MAX_RESULTS).toList(),
                postService.searchPosts(kw, null).stream().limit(MAX_RESULTS).toList()
        );
    }

    public List<SuggestionDto> getSuggestions(String keyword) {
        if (keyword == null || keyword.isBlank()) return List.of();
        String kw = keyword.trim();
        Stream<SuggestionDto> artists = artistService.searchArtists(kw).stream()
                .limit(MAX_SUGGESTIONS)
                .map(a -> new SuggestionDto(a.getId(), a.getName(), "artist", a.getProfileImageUrl()));
        Stream<SuggestionDto> festivals = festivalService.searchFestivals(kw).stream()
                .limit(MAX_SUGGESTIONS)
                .map(f -> new SuggestionDto(f.getId(), f.getTitle(), "festival", f.getPosterUrl()));
        return Stream.concat(artists, festivals).toList();
    }
}
