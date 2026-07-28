package com.feple.feple_backend.global.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final FilterChain chain = mock(FilterChain.class);

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void 응답헤더에_requestId_설정() throws Exception {
        filter.doFilterInternal(request, response, chain);

        verify(response).setHeader(eq("X-Request-Id"), any());
    }

    @Test
    void requestId는_12자리_문자열() throws Exception {
        AtomicReference<String> captured = new AtomicReference<>();
        doAnswer(invocation -> {
            captured.set(invocation.getArgument(1));
            return null;
        }).when(response).setHeader(eq("X-Request-Id"), any());

        filter.doFilterInternal(request, response, chain);

        assertThat(captured.get()).hasSize(12);
    }

    @Test
    void 체인_호출후_MDC_클리어() throws Exception {
        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void 체인에서_예외발생해도_MDC_클리어() throws Exception {
        willThrow(new RuntimeException("체인 오류")).given(chain).doFilter(any(), any());

        assertThatThrownBy(() -> filter.doFilterInternal(request, response, chain))
                .isInstanceOf(RuntimeException.class);

        assertThat(MDC.get("requestId")).isNull();
    }
}
