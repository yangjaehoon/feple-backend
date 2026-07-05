package com.feple.feple_backend.artist.song.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.feple.feple_backend.artist.song.dto.YoutubeVideoDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class YoutubeSearchService {

    private static final String YOUTUBE_SEARCH_URL = "https://www.googleapis.com/youtube/v3/search";
    private static final String YOUTUBE_VIDEOS_URL = "https://www.googleapis.com/youtube/v3/videos";
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 10_000;

    @Value("${app.youtube.api-key:}")
    private String apiKey;

    private final RestClient restClient;

    public YoutubeSearchService(RestClient.Builder restClientBuilder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        requestFactory.setReadTimeout(READ_TIMEOUT_MS);
        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
    }

    /**
     * @param artistName 아티스트명 — Topic 채널 탐색에 사용
     * @param query      곡명 — Topic 채널 결과 필터링 or 키워드 검색에 사용
     */
    public List<YoutubeVideoDto> search(String artistName, String query) {
        log.debug("[YT] search called — artistName='{}', query='{}'", artistName, query);
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[YT] API key is blank — returning empty");
            return Collections.emptyList();
        }

        // 1) 아티스트명으로 YouTube Music Topic 채널 탐색
        String topicChannelId = findTopicChannelId(artistName);
        if (topicChannelId != null) {
            log.debug("[YT] Topic channel found: {}", topicChannelId);
            List<YoutubeVideoDto> channelVideos = searchByChannel(topicChannelId);
            log.debug("[YT] Channel videos count: {}", channelVideos.size());
            if (query != null && !query.isBlank()) {
                String lower = query.toLowerCase();
                List<YoutubeVideoDto> filtered = channelVideos.stream()
                        .filter(v -> v.getTitle().toLowerCase().contains(lower))
                        .toList();
                log.debug("[YT] Filtered by '{}': {} results", lower, filtered.size());
                return filtered.isEmpty() ? channelVideos : filtered;
            }
            return channelVideos;
        }

        // 2) Topic 채널 없음 → 곡명만으로 검색 (아티스트명 붙이면 API 품질 저하)
        String keywordQuery = (query != null && !query.isBlank()) ? query : artistName;
        log.debug("[YT] No Topic channel — keyword fallback: '{}'", keywordQuery);
        return searchByKeyword(artistName, keywordQuery);
    }

    private String findTopicChannelId(String artistName) {
        String uri = UriComponentsBuilder.fromUriString(YOUTUBE_SEARCH_URL)
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
                log.debug("[YT] channel candidate: id={}, title='{}'",
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
        String uri = UriComponentsBuilder.fromUriString(YOUTUBE_SEARCH_URL)
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
        String uri = UriComponentsBuilder.fromUriString(YOUTUBE_SEARCH_URL)
                .queryParam("part", "snippet")
                .queryParam("q", query)
                .queryParam("type", "video")
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

        // channelTitle 또는 영상 title에 아티스트명이 포함된 경우 우선 반환
        // (유통사 채널 업로드 시 channelTitle≠아티스트명이므로 title도 함께 확인)
        String lowerArtist = artistName.toLowerCase();
        List<YoutubeVideoDto> byArtist = all.stream()
                .filter(v -> v.getChannelTitle().toLowerCase().contains(lowerArtist)
                        || v.getTitle().toLowerCase().contains(lowerArtist))
                .toList();

        all.forEach(v -> log.debug("[YT] result: title='{}', channel='{}'", v.getTitle(), v.getChannelTitle()));
        log.debug("[YT] Keyword '{}': total={}, byArtist={}", query, all.size(), byArtist.size());
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

    public Optional<YoutubeVideoDto> fetchVideoByUrl(String videoUrlOrId) {
        if (apiKey == null || apiKey.isBlank()) return Optional.empty();
        String videoId = extractVideoId(videoUrlOrId);
        if (videoId == null) {
            log.warn("[YT] fetchVideo: could not extract video ID from '{}'", videoUrlOrId);
            return Optional.empty();
        }
        try {
            URI uri = UriComponentsBuilder.fromUriString(YOUTUBE_VIDEOS_URL)
                    .queryParam("part", "snippet")
                    .queryParam("id", videoId)
                    .queryParam("key", apiKey)
                    .build().encode().toUri();
            YoutubeVideoListResponse response = restClient.get().uri(uri).retrieve()
                    .body(YoutubeVideoListResponse.class);
            if (response == null || response.items() == null || response.items().isEmpty()) return Optional.empty();
            YoutubeVideoItem item = response.items().get(0);
            return Optional.of(YoutubeVideoDto.builder()
                    .videoId(item.id())
                    .title(HtmlUtils.htmlUnescape(item.snippet().title()))
                    .channelTitle(HtmlUtils.htmlUnescape(item.snippet().channelTitle()))
                    .thumbnailUrl(extractThumbnailUrl(item.snippet().thumbnails()))
                    .build());
        } catch (Exception e) {
            log.warn("[YT] fetchVideo failed for '{}': {}", videoUrlOrId, e.getMessage());
            return Optional.empty();
        }
    }

    private String extractVideoId(String input) {
        if (input == null || input.isBlank()) return null;
        input = input.trim();
        // 11자리 영상 ID 직접 입력
        if (input.matches("[a-zA-Z0-9_\\-]{11}")) return input;
        // URL에서 v= 파라미터 추출 (youtube.com/watch?v=ID, music.youtube.com/watch?v=ID)
        try {
            URI uri = new URI(input);
            String query = uri.getQuery();
            if (query != null) {
                for (String param : query.split("&")) {
                    if (param.startsWith("v=")) return param.substring(2);
                }
            }
            // youtu.be/ID 형태
            String path = uri.getPath();
            if (path != null) {
                String[] parts = path.split("/");
                for (int i = parts.length - 1; i >= 0; i--) {
                    if (parts[i].matches("[a-zA-Z0-9_\\-]{11}")) return parts[i];
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record YoutubeVideoListResponse(List<YoutubeVideoItem> items) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record YoutubeVideoItem(String id, YoutubeSnippet snippet) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record YoutubeSearchResponse(List<YoutubeItem> items) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record YoutubeItem(YoutubeItemId id, YoutubeSnippet snippet) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record YoutubeItemId(String videoId, String channelId) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record YoutubeSnippet(String title, String channelTitle, Map<String, Object> thumbnails) {}
}
