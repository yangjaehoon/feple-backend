package com.feple.feple_backend.admin.scraper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SsrfUrlValidatorTest {

    // ── 유효한 URL ──────────────────────────────────────────────────────────

    @Test
    void 공개_IP_http_URL은_통과() {
        // InetAddress.getByName("8.8.8.8") 는 IP 파싱, DNS 호출 없음
        assertThatCode(() -> SsrfUrlValidator.validate("http://8.8.8.8"))
                .doesNotThrowAnyException();
    }

    @Test
    void 공개_IP_https_URL은_통과() {
        assertThatCode(() -> SsrfUrlValidator.validate("https://8.8.8.8"))
                .doesNotThrowAnyException();
    }

    @Test
    void 포트와_경로가_포함된_공개_IP_URL은_통과() {
        assertThatCode(() -> SsrfUrlValidator.validate("http://8.8.8.8:80/search?q=feple"))
                .doesNotThrowAnyException();
    }

    // ── 잘못된 URI 형식 ─────────────────────────────────────────────────────

    @Test
    void 공백_포함_문자열은_유효하지_않은_URL_예외() {
        // URI.create() 가 IllegalArgumentException 던짐
        assertThatThrownBy(() -> SsrfUrlValidator.validate("not a url"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 URL입니다.");
    }

    // ── 허용되지 않는 스킴 ──────────────────────────────────────────────────

    @ParameterizedTest(name = "\"{0}\" 스킴 거부")
    @ValueSource(strings = {
        "ftp://8.8.8.8",
        "file:///etc/passwd",
        "javascript://foo",
        "data:text/plain,hello",
    })
    void http_https_외_스킴은_거부(String url) {
        assertThatThrownBy(() -> SsrfUrlValidator.validate(url))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("http/https URL만 허용됩니다.");
    }

    @Test
    void 스킴_없는_URL은_거부() {
        // URI.create("//8.8.8.8") → scheme=null
        assertThatThrownBy(() -> SsrfUrlValidator.validate("//8.8.8.8"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("http/https URL만 허용됩니다.");
    }

    // ── 호스트 없는 URL ─────────────────────────────────────────────────────

    @Test
    void 호스트_없는_URL은_거부() {
        // "http:path" 는 opaque URI (authority 없음) → getHost() == null
        assertThatThrownBy(() -> SsrfUrlValidator.validate("http:no-host"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("URL에 호스트가 없습니다.");
    }

    // ── 내부 네트워크 IP 차단 ────────────────────────────────────────────────

    @ParameterizedTest(name = "{0} 차단")
    @ValueSource(strings = {
        "http://127.0.0.1",           // loopback
        "http://127.0.0.1:8080/api",  // loopback + 포트
        "http://192.168.0.1",         // private class C
        "http://192.168.1.100",       // private class C
        "http://10.0.0.1",            // private class A
        "http://10.10.10.10",         // private class A
        "http://172.16.0.1",          // private class B
        "http://172.31.255.255",      // private class B
        "http://169.254.1.1",         // link-local
        "http://0.0.0.0",             // any-local
        "http://224.0.0.1",           // multicast
    })
    void 내부_네트워크_IP는_차단(String url) {
        assertThatThrownBy(() -> SsrfUrlValidator.validate(url))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("내부 네트워크 주소는 허용되지 않습니다.");
    }

    @Test
    void localhost_호스트명은_차단() {
        assertThatThrownBy(() -> SsrfUrlValidator.validate("http://localhost"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("내부 네트워크 주소는 허용되지 않습니다.");
    }

    @Test
    void localhost_포트_포함도_차단() {
        assertThatThrownBy(() -> SsrfUrlValidator.validate("http://localhost:3000/admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("내부 네트워크 주소는 허용되지 않습니다.");
    }

    @Test
    void IPv6_루프백은_차단() {
        // URI.create("http://[::1]").getHost() == "::1"
        assertThatThrownBy(() -> SsrfUrlValidator.validate("http://[::1]"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("내부 네트워크 주소는 허용되지 않습니다.");
    }

    // ── 존재하지 않는 호스트 ─────────────────────────────────────────────────

    @Test
    void 존재하지_않는_호스트는_거부() {
        // RFC 6761: .invalid TLD 는 절대 등록되지 않음 → UnknownHostException 보장
        assertThatThrownBy(() -> SsrfUrlValidator.validate("http://nonexistent.invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("호스트를 찾을 수 없습니다");
    }
}
