package com.feple.feple_backend.artist.song.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.feple.feple_backend.artist.song.dto.YoutubeVideoDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;
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

    /**
     * @param artistName 아티스트명 — Topic 채널 탐색에 사용
     * @param query      곡명 — Topic 채널 결과 필터링 or 키워드 검색에 사용
     */
    public List<YoutubeVideoDto> search(String artistName, String query) {
        if (apiKey == null || apiKey.isBlank()) return Collections.emptyList();

        // 1) 아티스트명으로 YouTube Music Topic 채널 탐색
        String topicChannelId = findTopicChannelId(artistName);
        if (topicChannelId != null) {
            List<YoutubeVideoDto> channelVideos = searchByChannel(topicChannelId);
            // 곡명 입력이 있으면 채널 결과 내에서 필터링
            if (query != null && !query.isBlank()) {
                String lower = query.toLowerCase();
                List<YoutubeVideoDto> filtered = channelVideos.stream()
                        .filter(v -> v.getTitle().toLowerCase().contains(lower))
                        .toList();
                // 필터 결과가 없으면 필터 없이 전체 반환 (오타 등 대비)
                return filtered.isEmpty() ? channelVideos : filtered;
            }
            return channelVideos;
        }

        // 2) Topic 채널 없음 → "아티스트명 + 곡명" 키워드 검색
        String fullQuery = (artistName + " " + (query != null ? query : "")).trim();
        return searchByKeyword(fullQuery);
    }

    private String findTopicChannelId(String artistName) {
        String uri = UriComponentsBuilder.fromHttpUrl(YOUTUBE_SEARCH_URL)
                .queryParam("part", "snippet")
                .queryParam("q", artistName)
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

    private List<YoutubeVideoDto> searchByChannel(String channelId) {
        String uri = UriComponentsBuilder.fromHttpUrl(YOUTUBE_SEARCH_URL)
                .queryParam("part", "snippet")
                .queryParam("channelId", channelId)
                .queryParam("type", "video")
                .queryParam("order", "date")
                .queryParam("maxResults", "50")
                .queryParam("key", apiKey)
                .toUriString();

        YoutubeSearchResponse response = restClient.get().uri(uri).retrieve()
                .body(YoutubeSearchResponse.class);

        if (response == null || response.items() == null) return Collections.emptyList();

        return response.items().stream()
                .filter(item -> item.id() != null && item.id().videoId() != null)
                .map(this::toDto)
                .toList();
    }

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
                .map(this::toDto)
                .toList();
    }

    private YoutubeVideoDto toDto(YoutubeItem item) {
        return YoutubeVideoDto.builder()
                .videoId(item.id().videoId())
                .title(HtmlUtils.htmlUnescape(item.snippet().title()))
                .channelTitle(HtmlUtils.htmlUnescape(item.snippet().channelTitle()))
                .thumbnailUrl(extractThumbnailUrl(item.snippet().thumbnails()))
                .build();
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
