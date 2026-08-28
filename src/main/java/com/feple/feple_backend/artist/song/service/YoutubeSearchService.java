package com.feple.feple_backend.artist.song.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.feple.feple_backend.artist.song.dto.YoutubeVideoDto;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
public class YoutubeSearchService {

    private static final String YOUTUBE_SEARCH_URL = "https://www.googleapis.com/youtube/v3/search";
    private static final String YOUTUBE_VIDEOS_URL = "https://www.googleapis.com/youtube/v3/videos";
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 10_000;

    private final String apiKey;
    private final RestClient restClient;

    public YoutubeSearchService(RestClient.Builder restClientBuilder, YoutubeProperties youtubeProperties) {
        this.apiKey = youtubeProperties.apiKey();
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
        if (isApiKeyMissing()) return Collections.emptyList();

        try {
            // 1) 아티스트명으로 YouTube Music Topic 채널 탐색
            List<YoutubeVideoDto> viaTopicChannel = searchViaTopicChannel(artistName, query);
            if (viaTopicChannel != null) return viaTopicChannel;

            // 2) Topic 채널 없음 → 곡명만으로 검색 (아티스트명 붙이면 API 품질 저하)
            String keywordQuery = (query != null && !query.isBlank()) ? query : artistName;
            log.debug("[YT] No Topic channel — keyword fallback: '{}'", keywordQuery);
            return searchByKeyword(artistName, keywordQuery);
        } catch (Exception e) {
            // 요청 URI에 API 키가 쿼리파라미터로 포함돼 있어 e.getMessage()/스택트레이스 그대로 로깅하면
            // 키가 로그에 노출된다 — 예외 종류만 남긴다
            log.warn("[YT] search 실패 - artistName='{}', query='{}', 원인={}", artistName, query, e.getClass().getSimpleName());
            return Collections.emptyList();
        }
    }

    private boolean isApiKeyMissing() {
        if (apiKey != null && !apiKey.isBlank()) return false;
        // 빈 결과가 "검색 결과 없음"과 구분이 안 되므로 error로 남겨 눈에 띄게 함
        log.error("[YT] API key is blank — returning empty");
        return true;
    }

    // part/key 파라미터는 모든 YouTube Data API 요청에 공통이므로 한 곳에서만 조립한다.
    private UriComponentsBuilder apiRequest(String url) {
        return UriComponentsBuilder.fromUriString(url)
                .queryParam("part", "snippet")
                .queryParam("key", apiKey);
    }

    // Topic 채널이 있으면 해당 채널의 영상만 검색해 반환하고, 없으면 null을 반환해
    // 호출부가 키워드 검색으로 폴백하도록 한다.
    private List<YoutubeVideoDto> searchViaTopicChannel(String artistName, String query) {
        String topicChannelId = findTopicChannelId(artistName);
        if (topicChannelId == null) return null;

        log.debug("[YT] Topic channel found: {}", topicChannelId);
        List<YoutubeVideoDto> channelVideos = searchByChannel(topicChannelId);
        log.debug("[YT] Channel videos count: {}", channelVideos.size());
        return filterByQuery(channelVideos, query);
    }

    // query가 없으면 채널 영상 전체 반환, 있으면 제목에 포함되는 것만 — 필터링 결과가 없으면 전체로 폴백
    private List<YoutubeVideoDto> filterByQuery(List<YoutubeVideoDto> videos, String query) {
        if (query == null || query.isBlank()) return videos;
        String lower = query.toLowerCase();
        List<YoutubeVideoDto> filtered = videos.stream()
                .filter(v -> v.getTitle().toLowerCase().contains(lower))
                .toList();
        log.debug("[YT] Filtered by '{}': {} results", lower, filtered.size());
        return filtered.isEmpty() ? videos : filtered;
    }

    private String findTopicChannelId(String artistName) {
        String uri = apiRequest(YOUTUBE_SEARCH_URL)
                .queryParam("q", artistName)
                .queryParam("type", "channel")
                .queryParam("maxResults", "5")
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
        String uri = apiRequest(YOUTUBE_SEARCH_URL)
                .queryParam("channelId", channelId)
                .queryParam("type", "video")
                .queryParam("order", "date")
                .queryParam("maxResults", "50")
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
        String uri = apiRequest(YOUTUBE_SEARCH_URL)
                .queryParam("q", query)
                .queryParam("type", "video")
                .queryParam("maxResults", "25")
                .toUriString();

        YoutubeSearchResponse response = restClient.get().uri(uri).retrieve()
                .body(YoutubeSearchResponse.class);

        if (response == null || response.items() == null) return Collections.emptyList();

        List<YoutubeVideoDto> all = response.items().stream()
                .filter(item -> item.id() != null && item.id().videoId() != null)
                .map(this::toDto)
                .toList();

        return prioritizeByArtist(all, artistName, query);
    }

    // channelTitle 또는 영상 title에 아티스트명이 포함된 경우 우선 반환
    // (유통사 채널 업로드 시 channelTitle≠아티스트명이므로 title도 함께 확인)
    private List<YoutubeVideoDto> prioritizeByArtist(List<YoutubeVideoDto> all, String artistName, String query) {
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
        if (isApiKeyMissing()) return Optional.empty();
        String videoId = extractVideoId(videoUrlOrId);
        if (videoId == null) {
            log.warn("[YT] fetchVideo: could not extract video ID from '{}'", videoUrlOrId);
            return Optional.empty();
        }
        try {
            return callVideosApi(videoId);
        } catch (Exception e) {
            // API 키가 담긴 요청 URI가 예외 메시지에 포함될 수 있어 e.getMessage()는 로깅하지 않는다
            log.warn("[YT] fetchVideo 실패 - videoUrlOrId='{}', 원인={}", videoUrlOrId, e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private Optional<YoutubeVideoDto> callVideosApi(String videoId) {
        URI uri = apiRequest(YOUTUBE_VIDEOS_URL)
                .queryParam("id", videoId)
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
    }

    private static final String VIDEO_ID_PATTERN = "[a-zA-Z0-9_\\-]{11}";

    private String extractVideoId(String input) {
        if (input == null || input.isBlank()) return null;
        String trimmed = input.trim();
        if (trimmed.matches(VIDEO_ID_PATTERN)) return trimmed;

        try {
            URI uri = new URI(trimmed);
            String fromQuery = extractFromQueryParam(uri);
            if (fromQuery != null) return fromQuery;
            return extractFromPathSegment(uri);
        } catch (Exception ignored) {
            return null;
        }
    }

    // youtube.com/watch?v=ID, music.youtube.com/watch?v=ID
    private String extractFromQueryParam(URI uri) {
        String query = uri.getQuery();
        if (query == null) return null;
        for (String param : query.split("&")) {
            if (param.startsWith("v=")) return param.substring(2);
        }
        return null;
    }

    // youtu.be/ID 형태
    private String extractFromPathSegment(URI uri) {
        String path = uri.getPath();
        if (path == null) return null;
        String[] parts = path.split("/");
        for (int i = parts.length - 1; i >= 0; i--) {
            if (parts[i].matches(VIDEO_ID_PATTERN)) return parts[i];
        }
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
