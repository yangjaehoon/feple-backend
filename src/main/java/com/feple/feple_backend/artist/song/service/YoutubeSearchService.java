package com.feple.feple_backend.artist.song.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.feple.feple_backend.artist.song.dto.YoutubeVideoDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class YoutubeSearchService {

    private static final String YOUTUBE_SEARCH_URL = "https://www.googleapis.com/youtube/v3/search";

    @Value("${app.youtube.api-key:}")
    private String apiKey;

    private final RestClient restClient = RestClient.create();

    public List<YoutubeVideoDto> search(String query) {
        if (apiKey == null || apiKey.isBlank()) {
            return Collections.emptyList();
        }

        String uri = UriComponentsBuilder.fromHttpUrl(YOUTUBE_SEARCH_URL)
                .queryParam("part", "snippet")
                .queryParam("q", query)
                .queryParam("type", "video")
                .queryParam("videoCategoryId", "10")
                .queryParam("maxResults", "10")
                .queryParam("key", apiKey)
                .toUriString();

        YoutubeSearchResponse response = restClient.get()
                .uri(uri)
                .retrieve()
                .body(YoutubeSearchResponse.class);

        if (response == null || response.items() == null) {
            return Collections.emptyList();
        }

        return response.items().stream()
                .filter(item -> item.id() != null && item.id().videoId() != null)
                .map(item -> YoutubeVideoDto.builder()
                        .videoId(item.id().videoId())
                        .title(item.snippet().title())
                        .channelTitle(item.snippet().channelTitle())
                        .thumbnailUrl(extractThumbnailUrl(item.snippet().thumbnails()))
                        .build())
                .toList();
    }

    private String extractThumbnailUrl(Map<String, Object> thumbnails) {
        if (thumbnails == null) return null;
        var medium = thumbnails.get("medium");
        if (medium instanceof Map<?, ?> m) {
            Object url = m.get("url");
            if (url instanceof String s) return s;
        }
        var defaultThumb = thumbnails.get("default");
        if (defaultThumb instanceof Map<?, ?> m) {
            Object url = m.get("url");
            if (url instanceof String s) return s;
        }
        return null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record YoutubeSearchResponse(List<YoutubeItem> items) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record YoutubeItem(YoutubeItemId id, YoutubeSnippet snippet) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record YoutubeItemId(String videoId) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record YoutubeSnippet(String title, String channelTitle, Map<String, Object> thumbnails) {}
}
