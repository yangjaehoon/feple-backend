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
        // Topic 채널(공식 음원 채널) 우선 시도 → 없으면 키워드 검색
        String topicChannelId = findTopicChannelId(query);
        if (topicChannelId != null) {
            return searchByChannel(topicChannelId);
        }
        return searchByKeyword(query);
    }

    // "아티스트명 - Topic" 채널 ID 조회
    private String findTopicChannelId(String query) {
        String uri = UriComponentsBuilder.fromHttpUrl(YOUTUBE_SEARCH_URL)
                .queryParam("part", "snippet")
                .queryParam("q", query)
                .queryParam("type", "channel")
                .queryParam("maxResults", "5")
                .queryParam("key", apiKey)
                .toUriString();

        YoutubeSearchResponse response = restClient.get().uri(uri).retrieve()
                .body(YoutubeSearchResponse.class);

        if (response == null || response.items() == null) return null;

        return response.items().stream()
                .filter(item -> item.id() != null && item.id().channelId() != null)
                .filter(item -> item.snippet() != null
                        && item.snippet().channelTitle() != null
                        && item.snippet().channelTitle().toLowerCase().contains("- topic"))
                .map(item -> item.id().channelId())
                .findFirst()
                .orElse(null);
    }

    // Topic 채널의 동영상 목록 조회
    private List<YoutubeVideoDto> searchByChannel(String channelId) {
        String uri = UriComponentsBuilder.fromHttpUrl(YOUTUBE_SEARCH_URL)
                .queryParam("part", "snippet")
                .queryParam("channelId", channelId)
                .queryParam("type", "video")
                .queryParam("order", "date")
                .queryParam("maxResults", "20")
                .queryParam("key", apiKey)
                .toUriString();

        YoutubeSearchResponse response = restClient.get().uri(uri).retrieve()
                .body(YoutubeSearchResponse.class);

        if (response == null || response.items() == null) return Collections.emptyList();

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

    // Topic 채널이 없을 때 키워드 검색 (음악 토픽 필터 적용)
    private List<YoutubeVideoDto> searchByKeyword(String query) {
        String uri = UriComponentsBuilder.fromHttpUrl(YOUTUBE_SEARCH_URL)
                .queryParam("part", "snippet")
                .queryParam("q", query)
                .queryParam("type", "video")
                .queryParam("topicId", "/m/04rlf")
                .queryParam("maxResults", "15")
                .queryParam("key", apiKey)
                .toUriString();

        YoutubeSearchResponse response = restClient.get().uri(uri).retrieve()
                .body(YoutubeSearchResponse.class);

        if (response == null || response.items() == null) return Collections.emptyList();

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
        for (String key : List.of("medium", "default", "high")) {
            var thumb = thumbnails.get(key);
            if (thumb instanceof Map<?, ?> m && m.get("url") instanceof String s) return s;
        }
        return null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record YoutubeSearchResponse(List<YoutubeItem> items) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record YoutubeItem(YoutubeItemId id, YoutubeSnippet snippet) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record YoutubeItemId(String videoId, String channelId) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record YoutubeSnippet(String title, String channelTitle, Map<String, Object> thumbnails) {}
}
