package com.feple.feple_backend.admin.scraper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SsrfSafeDnsResolverTest {

    private final SsrfSafeDnsResolver resolver = new SsrfSafeDnsResolver();

    @Test
    void 공개_IP는_통과() {
        assertThatCode(() -> resolver.resolve("8.8.8.8"))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0} 차단")
    @ValueSource(strings = {
        "127.0.0.1",   // loopback
        "192.168.0.1", // private class C
        "10.0.0.1",    // private class A
        "172.16.0.1",  // private class B
        "169.254.1.1", // link-local
        "0.0.0.0",     // any-local
        "224.0.0.1",   // multicast
    })
    void 내부_네트워크_IP는_UnknownHostException(String ip) {
        assertThatThrownBy(() -> resolver.resolve(ip))
                .isInstanceOf(UnknownHostException.class)
                .hasMessageContaining("내부 네트워크 주소로 연결할 수 없습니다");
    }

    @Test
    void resolveCanonicalHostname은_delegate에_위임() throws UnknownHostException {
        assertThatCode(() -> resolver.resolveCanonicalHostname("8.8.8.8"))
                .doesNotThrowAnyException();
    }
}
