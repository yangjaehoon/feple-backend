package com.feple.feple_backend.artist.song.service;

import com.feple.feple_backend.artist.song.dto.YoutubeVideoDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class YoutubeSearchServiceTest {

    private ClientHttpRequestInterceptor jsonInterceptor(Function<String, String> responseByUri) {
        return (request, body, execution) -> {
            MockClientHttpResponse response = new MockClientHttpResponse(
                    responseByUri.apply(request.getURI().toString()).getBytes(StandardCharsets.UTF_8), HttpStatus.OK);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return response;
        };
    }

    private YoutubeSearchService serviceWithResponses(String apiKey, Function<String, String> responseByUri) {
        YoutubeSearchService service = new YoutubeSearchService(
                RestClient.builder().requestInterceptor(jsonInterceptor(responseByUri)));
        ReflectionTestUtils.setField(service, "apiKey", apiKey);
        return service;
    }

    private YoutubeSearchService serviceCapturingUri(String apiKey, String json, AtomicReference<String> capturedUri) {
        ClientHttpRequestInterceptor interceptor = (request, body, execution) -> {
            capturedUri.set(request.getURI().toString());
            MockClientHttpResponse response = new MockClientHttpResponse(json.getBytes(StandardCharsets.UTF_8), HttpStatus.OK);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return response;
        };
        YoutubeSearchService service = new YoutubeSearchService(RestClient.builder().requestInterceptor(interceptor));
        ReflectionTestUtils.setField(service, "apiKey", apiKey);
        return service;
    }

    // ── search ───────────────────────────────────────────────────────────

    @Test
    void search_API키_없으면_빈목록() {
        YoutubeSearchService service = new YoutubeSearchService(RestClient.builder());
        ReflectionTestUtils.setField(service, "apiKey", "");

        assertThat(service.search("아티스트", "곡명")).isEmpty();
    }

    @Test
    void search_Topic채널_있으면_채널영상중_쿼리에_맞는것만_반환() {
        String channelSearchJson = "{\"items\":[{\"id\":{\"channelId\":\"UC123\"},"
                + "\"snippet\":{\"channelTitle\":\"아티스트 - Topic\"}}]}";
        String channelVideosJson = "{\"items\":["
                + "{\"id\":{\"videoId\":\"v1\"},\"snippet\":{\"title\":\"곡명 (Official)\",\"channelTitle\":\"아티스트 - Topic\","
                + "\"thumbnails\":{\"medium\":{\"url\":\"https://img/v1.jpg\"}}}},"
                + "{\"id\":{\"videoId\":\"v2\"},\"snippet\":{\"title\":\"다른곡\",\"channelTitle\":\"아티스트 - Topic\",\"thumbnails\":{}}}"
                + "]}";
        YoutubeSearchService service = serviceWithResponses("key",
                uri -> uri.contains("type=channel") ? channelSearchJson : channelVideosJson);

        List<YoutubeVideoDto> result = service.search("아티스트", "곡명");

        assertThat(result).extracting(YoutubeVideoDto::getVideoId).containsExactly("v1");
        assertThat(result.get(0).getThumbnailUrl()).isEqualTo("https://img/v1.jpg");
    }

    @Test
    void search_Topic채널_있고_쿼리에_맞는_영상없으면_전체반환() {
        String channelSearchJson = "{\"items\":[{\"id\":{\"channelId\":\"UC123\"},"
                + "\"snippet\":{\"channelTitle\":\"아티스트 - Topic\"}}]}";
        String channelVideosJson = "{\"items\":["
                + "{\"id\":{\"videoId\":\"v1\"},\"snippet\":{\"title\":\"엉뚱한제목\",\"channelTitle\":\"아티스트 - Topic\",\"thumbnails\":{}}}"
                + "]}";
        YoutubeSearchService service = serviceWithResponses("key",
                uri -> uri.contains("type=channel") ? channelSearchJson : channelVideosJson);

        List<YoutubeVideoDto> result = service.search("아티스트", "존재안함쿼리");

        assertThat(result).extracting(YoutubeVideoDto::getVideoId).containsExactly("v1");
    }

    @Test
    void search_Topic채널_없으면_키워드검색에서_아티스트명_매칭_우선반환() {
        String noChannelJson = "{\"items\":[]}";
        String keywordSearchJson = "{\"items\":["
                + "{\"id\":{\"videoId\":\"v1\"},\"snippet\":{\"title\":\"곡명 by 다른유통사\",\"channelTitle\":\"유통사채널\",\"thumbnails\":{}}},"
                + "{\"id\":{\"videoId\":\"v2\"},\"snippet\":{\"title\":\"곡명 - 아티스트\",\"channelTitle\":\"아티스트 official\",\"thumbnails\":{}}}"
                + "]}";
        YoutubeSearchService service = serviceWithResponses("key",
                uri -> uri.contains("type=channel") ? noChannelJson : keywordSearchJson);

        List<YoutubeVideoDto> result = service.search("아티스트", "곡명");

        assertThat(result).extracting(YoutubeVideoDto::getVideoId).containsExactly("v2");
    }

    @Test
    void search_Topic채널_없고_아티스트명_매칭도_없으면_전체결과반환() {
        String noChannelJson = "{\"items\":[]}";
        String keywordSearchJson = "{\"items\":["
                + "{\"id\":{\"videoId\":\"v1\"},\"snippet\":{\"title\":\"곡명\",\"channelTitle\":\"무관채널\",\"thumbnails\":{}}}"
                + "]}";
        YoutubeSearchService service = serviceWithResponses("key",
                uri -> uri.contains("type=channel") ? noChannelJson : keywordSearchJson);

        List<YoutubeVideoDto> result = service.search("아티스트", "곡명");

        assertThat(result).extracting(YoutubeVideoDto::getVideoId).containsExactly("v1");
    }

    // ── fetchVideoByUrl ──────────────────────────────────────────────────

    @Test
    void fetchVideo_API키_없으면_빈값() {
        YoutubeSearchService service = new YoutubeSearchService(RestClient.builder());
        ReflectionTestUtils.setField(service, "apiKey", "");

        assertThat(service.fetchVideoByUrl("dQw4w9WgXcQ")).isEmpty();
    }

    @Test
    void fetchVideo_videoId_추출실패시_빈값() {
        YoutubeSearchService service = new YoutubeSearchService(RestClient.builder());
        ReflectionTestUtils.setField(service, "apiKey", "key");

        assertThat(service.fetchVideoByUrl("이것은 유효한 URL도 ID도 아님")).isEmpty();
    }

    @Test
    void fetchVideo_11자_ID_직접입력시_정상조회() {
        String json = "{\"items\":[{\"id\":\"dQw4w9WgXcQ\",\"snippet\":{\"title\":\"Never Gonna Give You Up\","
                + "\"channelTitle\":\"Rick Astley\",\"thumbnails\":{\"medium\":{\"url\":\"https://img/thumb.jpg\"}}}}]}";
        YoutubeSearchService service = serviceWithResponses("key", uri -> json);

        Optional<YoutubeVideoDto> result = service.fetchVideoByUrl("dQw4w9WgXcQ");

        assertThat(result).isPresent();
        assertThat(result.get().getVideoId()).isEqualTo("dQw4w9WgXcQ");
        assertThat(result.get().getTitle()).isEqualTo("Never Gonna Give You Up");
        assertThat(result.get().getThumbnailUrl()).isEqualTo("https://img/thumb.jpg");
    }

    @Test
    void fetchVideo_watch_URL에서_videoId_추출() {
        String json = "{\"items\":[{\"id\":\"dQw4w9WgXcQ\",\"snippet\":{\"title\":\"제목\",\"channelTitle\":\"채널\",\"thumbnails\":{}}}]}";
        AtomicReference<String> capturedUri = new AtomicReference<>();
        YoutubeSearchService service = serviceCapturingUri("key", json, capturedUri);

        Optional<YoutubeVideoDto> result = service.fetchVideoByUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=xyz");

        assertThat(result).isPresent();
        assertThat(capturedUri.get()).contains("id=dQw4w9WgXcQ");
    }

    @Test
    void fetchVideo_단축URL에서_videoId_추출() {
        String json = "{\"items\":[{\"id\":\"dQw4w9WgXcQ\",\"snippet\":{\"title\":\"제목\",\"channelTitle\":\"채널\",\"thumbnails\":{}}}]}";
        AtomicReference<String> capturedUri = new AtomicReference<>();
        YoutubeSearchService service = serviceCapturingUri("key", json, capturedUri);

        Optional<YoutubeVideoDto> result = service.fetchVideoByUrl("https://youtu.be/dQw4w9WgXcQ");

        assertThat(result).isPresent();
        assertThat(capturedUri.get()).contains("id=dQw4w9WgXcQ");
    }

    @Test
    void fetchVideo_응답이_비어있으면_빈값() {
        String json = "{\"items\":[]}";
        YoutubeSearchService service = serviceWithResponses("key", uri -> json);

        assertThat(service.fetchVideoByUrl("dQw4w9WgXcQ")).isEmpty();
    }

    @Test
    void fetchVideo_요청_실패시_예외삼키고_빈값_반환() {
        ClientHttpRequestInterceptor interceptor = (request, body, execution) -> {
            throw new IOException("network error");
        };
        YoutubeSearchService service = new YoutubeSearchService(RestClient.builder().requestInterceptor(interceptor));
        ReflectionTestUtils.setField(service, "apiKey", "key");

        assertThat(service.fetchVideoByUrl("dQw4w9WgXcQ")).isEmpty();
    }
}
