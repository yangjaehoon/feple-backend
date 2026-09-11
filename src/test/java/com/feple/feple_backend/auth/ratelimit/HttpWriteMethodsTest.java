package com.feple.feple_backend.auth.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class HttpWriteMethodsTest {

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE", "post", "put"})
    void 상태변경_메서드는_true(String method) {
        assertThat(HttpWriteMethods.isWriteMethod(method)).isTrue();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"GET", "HEAD", "OPTIONS"})
    void 안전한_메서드나_null은_false(String method) {
        assertThat(HttpWriteMethods.isWriteMethod(method)).isFalse();
    }
}
