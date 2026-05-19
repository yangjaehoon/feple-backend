package com.feple.feple_backend.artist.song.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.feple.feple_backend.artist.song.dto.YoutubeVideoDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
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
        log.info("[YT] search called — artistName='{}', query='{}'", artistName, query);
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[YT] API key is blank — returning empty");
            return Collections.emptyList();
        }

        // 1) 아티스트명으로 YouTube Music Topic 채널 탐색
        String topicChannelId = findTopicChannelId(artistName);
        if (topicChannelId != null) {
            log.info("[YT] Topic channel found: {}", topicChannelId);
            List<YoutubeVideoDto> channelVideos = searchByChannel(topicChannelId);
            log.info("[YT] Channel videos count: {}", channelVideos.size());
            if (query != null && !query.isBlank()) {
                String lower = query.toLowerCase();
                List<YoutubeVideoDto> filtered = channelVideos.stream()
                        .filter(v -> v.getTitle().toLowerCase().contains(lower))
                        .toList();
                log.info("[YT] Filtered by '{}': {} results", lower, filtered.size());
                return filtered.isEmpty() ? channelVideos : filtered;
            }
            return channelVideos;
        }

        // 2) Topic 채널 없음 → "아티스트명 + 곡명" 키워드 검색
        String fullQuery = (artistName + " " + (query != null ? query : "")).trim();
        log.info("[YT] No Topic channel — keyword fallback: '{}'", fullQuery);
        return searchByKeyword(artistName, fullQuery);
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

        response.items().forEach(item ->
                log.info("[YT] channel candidate: id={}, title='{}'",
                        item.id() != null ? item.id().channelId() : "null",
                        item.snippet() != null ? item.snippet().channelTitle() : "null"));

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

    private List<YoutubeVideoDto> searchByKeyword(String artistName, String query) {
        String uri = UriComponentsBuilder.fromHttpUrl(YOUTUBE_SEARCH_URL)
                .queryParam("part", "snippet")
                .queryParam("q", query)
                .queryParam("type", "video")
                .queryParam("videoCategoryId", "10")
                .queryParam("maxResults", "25")
                .queryParam("key", apiKey)
                .toUriString();

        YoutubeSearchResponse response = restClient.get().uri(uri).retrieve()
                .body(YoutubeSearchResponse.class);

        if (response == null || response.items() == null) return Collections.emptyList();

        List<YoutubeVideoDto> all = response.items().stream()
                .filter(item -> item.id() != null && item.id().videoId() != null)
                .map(this::toDto)
                .toList();

        // channelTitle에 아티스트명이 포함된 영상 우선 — 플레이리스트/컴필레이션 제거 목적
        String lowerArtist = artistName.toLowerCase();
        List<YoutubeVideoDto> byArtist = all.stream()
                .filter(v -> v.getChannelTitle().toLowerCase().contains(lowerArtist))
                .toList();

        log.info("[YT] Keyword '{}': total={}, byArtist={}", query, all.size(), byArtist.size());
        return byArtist.isEmpty() ? all : byArtist;
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
