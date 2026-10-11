package com.feple.feple_backend.auth.kakao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.feple.feple_backend.auth.dto.KakaoUserResponseDto;
import com.feple.feple_backend.global.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

class KakaoApiClientTest {

    private static final long APP_ID = 1234L;
    private static final String TOKEN_INFO_PATH = "/v1/user/access_token_info";

    private KakaoApiClient clientWith(ExchangeFunction exchangeFunction) {
        WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
        return new KakaoApiClient(webClient, new KakaoAuthProperties(APP_ID));
    }

    private static Mono<ClientResponse> json(HttpStatus status, String body) {
        return Mono.just(ClientResponse.create(status)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build());
    }

    /** 토큰 정보 조회는 [tokenAppId]로 응답하고, 그 외 요청은 [userMe]에 맡긴다. */
    private static ExchangeFunction tokenIssuedFor(long tokenAppId, ExchangeFunction userMe) {
        return request -> request.url().getPath().equals(TOKEN_INFO_PATH)
                ? json(HttpStatus.OK, "{\"id\":123,\"expires_in\":3600,\"app_id\":" + tokenAppId + "}")
                : userMe.exchange(request);
    }

    @Test
    void getMe_요청에_Bearer_토큰_헤더와_올바른_URI_포함() {
        ClientRequest[] captured = new ClientRequest[1];
        ExchangeFunction exchangeFunction = request -> {
            captured[0] = request;
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body("{\"id\":123,\"kakao_account\":{\"email\":\"test@test.com\"}}")
                    .build());
        };
        KakaoApiClient client = clientWith(tokenIssuedFor(APP_ID, exchangeFunction));

        client.getMe("access-token-123").block();

        assertThat(captured[0].url().toString()).isEqualTo("https://kapi.kakao.com/v2/user/me");
        assertThat(captured[0].headers().getFirst("Authorization")).isEqualTo("Bearer access-token-123");
        assertThat(captured[0].headers().getContentType()).isEqualTo(MediaType.APPLICATION_FORM_URLENCODED);
    }

    @Test
    void getMe_응답이_사용자_정보로_역직렬화됨() {
        ExchangeFunction exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body("{\"id\":123,\"kakao_account\":{\"email\":\"test@test.com\"}}")
                .build());
        KakaoApiClient client = clientWith(tokenIssuedFor(APP_ID, exchangeFunction));

        KakaoUserResponseDto result = client.getMe("access-token-123").block();

        assertThat(result.getId()).isEqualTo(123L);
        assertThat(result.getKakaoAccount().getEmail()).isEqualTo("test@test.com");
    }

    @Test
    void getMe_4xx_응답이면_InvalidRequestException으로_변환() {
        ExchangeFunction exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.UNAUTHORIZED)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body("{\"msg\":\"invalid token\"}")
                .build());
        KakaoApiClient client = clientWith(tokenIssuedFor(APP_ID, exchangeFunction));

        assertThatThrownBy(() -> client.getMe("bad-token").block())
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("카카오 로그인에 실패");
    }

    @Test
    void getMe_5xx_응답이면_WebClientResponseException_전파() {
        ExchangeFunction exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.BAD_GATEWAY)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body("{\"msg\":\"upstream error\"}")
                .build());
        KakaoApiClient client = clientWith(tokenIssuedFor(APP_ID, exchangeFunction));

        assertThatThrownBy(() -> client.getMe("token").block())
                .isInstanceOf(WebClientResponseException.class);
    }

    @Test
    void getMe_다른_앱에서_발급된_토큰이면_사용자정보_조회없이_InvalidRequestException() {
        boolean[] userMeCalled = {false};
        KakaoApiClient client = clientWith(tokenIssuedFor(9999L, request -> {
            userMeCalled[0] = true;
            return json(HttpStatus.OK, "{\"id\":123}");
        }));

        assertThatThrownBy(() -> client.getMe("other-app-token").block())
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("카카오 로그인에 실패");
        assertThat(userMeCalled[0]).isFalse();
    }

    @Test
    void getMe_토큰정보_조회가_4xx면_사용자정보_조회없이_InvalidRequestException() {
        boolean[] userMeCalled = {false};
        KakaoApiClient client = clientWith(request -> {
            if (request.url().getPath().equals(TOKEN_INFO_PATH)) {
                return json(HttpStatus.UNAUTHORIZED, "{\"msg\":\"invalid token\"}");
            }
            userMeCalled[0] = true;
            return json(HttpStatus.OK, "{\"id\":123}");
        });

        assertThatThrownBy(() -> client.getMe("bad-token").block())
                .isInstanceOf(InvalidRequestException.class);
        assertThat(userMeCalled[0]).isFalse();
    }
}
