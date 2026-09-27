package com.feple.feple_backend.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS 허용 출처 가드 — 잘못된 설정으로 조용히 부팅되면 운영에서야 드러나므로 기동 실패로 막는다.
 * 의존성은 생성자에서 필드 대입만 하므로 null로 넘겨도 corsConfigurationSource() 검증에 지장이 없다.
 */
class SecurityConfigCorsTest {

    private SecurityConfig configWithOrigins(String origins) {
        SecurityConfig config =
                new SecurityConfig(null, null, null, null, null, null, null, null, null);
        ReflectionTestUtils.setField(config, "allowedOrigins", origins);
        return config;
    }

    private java.util.List<String> patternsOf(SecurityConfig config) {
        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) config.corsConfigurationSource();
        CorsConfiguration cors = source.getCorsConfigurations().get("/**");
        return cors.getAllowedOriginPatterns();
    }

    @Test
    void 정상_출처는_패턴으로_등록된다() {
        var config = configWithOrigins("https://feple.com, https://admin.feple.com");

        assertThat(patternsOf(config))
                .containsExactly("https://feple.com", "https://admin.feple.com");
    }

    // "".split(",")는 [""]를 돌려주므로, 빈 항목을 걸러내지 않으면 "출처 없음" 가드를 통과해
    // 아무 출처도 매칭되지 않는 설정으로 기동한다(모든 브라우저 클라이언트가 CORS 차단).
    @Test
    void 설정이_비어_있으면_기동에_실패한다() {
        assertThatThrownBy(() -> configWithOrigins("").corsConfigurationSource())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CORS 허용 출처가 없습니다");
    }

    @Test
    void 공백과_쉼표만_있어도_기동에_실패한다() {
        assertThatThrownBy(() -> configWithOrigins(" , , ").corsConfigurationSource())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CORS 허용 출처가 없습니다");
    }

    // setAllowedOriginPatterns는 '*'를 와일드카드로 해석한다 — "*" 문자열만 막으면
    // "https://*"로 모든 출처가 허용된다.
    @Test
    void 호스트가_통째로_와일드카드면_기동에_실패한다() {
        assertThatThrownBy(() -> configWithOrigins("https://*").corsConfigurationSource())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("모든 출처를 허용하는 CORS 패턴");

        assertThatThrownBy(() -> configWithOrigins("http://*:8080").corsConfigurationSource())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("모든 출처를 허용하는 CORS 패턴");

        assertThatThrownBy(() -> configWithOrigins("*").corsConfigurationSource())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("모든 출처를 허용하는 CORS 패턴");
    }

    @Test
    void 서브도메인_와일드카드는_허용한다() {
        var config = configWithOrigins("https://*.feple.com");

        assertThat(patternsOf(config)).containsExactly("https://*.feple.com");
    }

    @Test
    void 정상_출처에_와일드카드가_하나라도_섞이면_기동에_실패한다() {
        assertThatThrownBy(
                        () -> configWithOrigins("https://feple.com, https://*")
                                .corsConfigurationSource())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("https://*");
    }
}
