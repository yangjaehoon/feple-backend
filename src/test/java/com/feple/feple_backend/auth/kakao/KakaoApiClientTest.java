package com.feple.feple_backend.auth.kakao;

import com.feple.feple_backend.auth.dto.KakaoUserResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KakaoApiClientTest {

    private KakaoApiClient clientWith(ExchangeFunction exchangeFunction) {
        WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
        return new KakaoApiClient(webClient);
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
        KakaoApiClient client = clientWith(exchangeFunction);

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
        KakaoApiClient client = clientWith(exchangeFunction);

        KakaoUserResponseDto result = client.getMe("access-token-123").block();

        assertThat(result.getId()).isEqualTo(123L);
        assertThat(result.getKakaoAccount().getEmail()).isEqualTo("test@test.com");
    }

    @Test
    void getMe_401_응답이면_예외_전파() {
        ExchangeFunction exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.UNAUTHORIZED)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body("{\"msg\":\"invalid token\"}")
                .build());
        KakaoApiClient client = clientWith(exchangeFunction);

        assertThatThrownBy(() -> client.getMe("bad-token").block())
                .isInstanceOf(WebClientResponseException.class);
    }
}
