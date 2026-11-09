package com.feple.feple_backend.admin.ocr;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;

class GeminiFailureTest {

    private static WebClientResponseException googleError(HttpStatus status) {
        return WebClientResponseException.create(status.value(), status.getReasonPhrase(), HttpHeaders.EMPTY, new byte[0], null);
    }

    @Test
    void of_구글_상태코드별로_실패_종류를_구분한다() {
        assertThat(GeminiFailure.of(googleError(HttpStatus.SERVICE_UNAVAILABLE))).isEqualTo(GeminiFailure.OVERLOADED);
        assertThat(GeminiFailure.of(googleError(HttpStatus.TOO_MANY_REQUESTS))).isEqualTo(GeminiFailure.RATE_LIMITED);
        assertThat(GeminiFailure.of(googleError(HttpStatus.FORBIDDEN))).isEqualTo(GeminiFailure.FORBIDDEN);
        assertThat(GeminiFailure.of(googleError(HttpStatus.NOT_FOUND))).isEqualTo(GeminiFailure.OTHER);
    }

    @Test
    void of_block_제한시간_초과는_TIMEOUT이다() {
        Exception timeout = new IllegalStateException("Timeout on blocking read", new TimeoutException());

        assertThat(GeminiFailure.of(timeout)).isEqualTo(GeminiFailure.TIMEOUT);
    }

    @Test
    void of_알_수_없는_예외는_OTHER이고_예비_모델로_넘기지_않는다() {
        GeminiFailure failure = GeminiFailure.of(new IllegalStateException("boom"));

        assertThat(failure).isEqualTo(GeminiFailure.OTHER);
        assertThat(failure.allowsFallback()).isFalse();
    }

    @Test
    void toResponse_원본_예외_없이_안내_문구와_상태코드를_반환한다() {
        var response = GeminiFailure.RATE_LIMITED.toResponse();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody().message()).contains("한도");
    }
}
