package com.feple.feple_backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feple.feple_backend.admin.AdminLoginFailureHandler;
import com.feple.feple_backend.auth.jwt.JwtAuthenticationFilter;
import com.feple.feple_backend.auth.jwt.JwtProvider;
import com.feple.feple_backend.global.exception.ErrorCode;
import com.feple.feple_backend.global.exception.ErrorResponse;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final AdminLoginFailureHandler adminLoginFailureHandler;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.cors.allowed-origins:http://localhost:8080}")
    private String allowedOrigins;

    @Value("${app.s3.bucket:}")
    private String s3Bucket;

    @Value("${app.cdn.base-url:}")
    private String cdnBaseUrl;

    // ── 0. Swagger UI 전용 FilterChain (로컬 개발 환경에서만 활성화) ──
    @Bean
    @Order(1)
    public SecurityFilterChain swaggerFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; " +
                    "script-src 'self' 'unsafe-inline'; " +
                    "style-src 'self' 'unsafe-inline'; " +
                    "img-src 'self' data:; " +
                    "connect-src 'self'"))
                .frameOptions(frame -> frame.sameOrigin()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    // ── 1. 관리자 페이지용 FilterChain (세션 기반 폼 로그인, CSRF 활성화) ──
    @Bean
    @Order(2)
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/admin/**", "/css/**", "/js/**", "/img/**")
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(buildAdminCsp()))
                .frameOptions(frame -> frame.deny()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/img/**").permitAll()
                .anyRequest().hasRole("ADMIN"))
            .formLogin(form -> form
                .loginPage("/admin/login")
                .loginProcessingUrl("/admin/login")
                .defaultSuccessUrl("/admin", true)
                .failureHandler(adminLoginFailureHandler)
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/admin/logout")
                .logoutSuccessUrl("/admin/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll())
            .sessionManagement(sm -> sm
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation().changeSessionId()
                .maximumSessions(1)
                .expiredUrl("/admin/login?expired=true"))
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/admin/access-denied"));

        return http.build();
    }

    // ── 2. API용 FilterChain (JWT Stateless) ──
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtProvider, userRepository);
    }

    // Spring Boot가 Filter Bean을 서블릿 체인에 자동 등록하지 않도록 비활성화
    // — Spring Security 필터 체인에서만 동작해야 이중 실행을 방지할 수 있음
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    @Order(3)
    public SecurityFilterChain apiFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        http
            .headers(headers -> headers
                .contentTypeOptions(ct -> {})          // X-Content-Type-Options: nosniff
                .frameOptions(frame -> frame.deny())   // X-Frame-Options: DENY
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'")))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/users/check-nickname").permitAll()
                .requestMatchers("/favicon.ico", "/error").permitAll()
                // /posts/my/** 전체 및 presigned URL은 인증 필수
                .requestMatchers(HttpMethod.GET, "/posts/my/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/festivals/**", "/artists/**",
                    "/posts/**", "/comments/**").permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/certifications/festival/*/rating",
                    "/certifications/festival/*/reviews").permitAll()
                .anyRequest().authenticated())
            .httpBasic(hb -> hb.disable())
            .formLogin(fl -> fl.disable())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> {
                    // JwtAuthenticationFilter가 만료/변조/정지 등 구체적인 실패 사유를 request attribute로
                    // 남겨뒀다면(보호된 엔드포인트라 인증이 필요해진 경우) 그 사유를 그대로 응답한다.
                    // 토큰 자체가 없었던 경우(비로그인)는 일반적인 로그인 필요 메시지를 사용한다.
                    Object failureAttr = req.getAttribute(JwtAuthenticationFilter.JWT_FAILURE_ATTRIBUTE);
                    HttpStatus status = HttpStatus.UNAUTHORIZED;
                    String message = "로그인이 필요합니다.";
                    ErrorCode code = ErrorCode.UNAUTHORIZED;
                    if (failureAttr instanceof JwtAuthenticationFilter.JwtFailure failure) {
                        status = failure.status();
                        message = failure.message();
                        code = failure.code();
                    }
                    ErrorResponse body = ErrorResponse.of(status, message, code);
                    res.setStatus(status.value());
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
                    res.getWriter().write(objectMapper.writeValueAsString(body));
                }))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(o -> !o.equals("*"))
                .toList();
        if (origins.isEmpty()) {
            throw new IllegalStateException(
                    "CORS 허용 출처가 없습니다. CORS_ALLOWED_ORIGINS 환경변수를 확인하세요. 와일드카드 '*'는 허용되지 않습니다.");
        }
        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept"));
        config.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private String buildAdminCsp() {
        String s3Origin = (s3Bucket == null || s3Bucket.isBlank())
                ? "https://*.s3.ap-northeast-2.amazonaws.com"
                : "https://" + s3Bucket + ".s3.ap-northeast-2.amazonaws.com";
        String cdnOrigin = (cdnBaseUrl == null || cdnBaseUrl.isBlank()) ? "" : " " + cdnBaseUrl;
        return "default-src 'self'; " +
               "script-src 'self' 'unsafe-inline' https://dapi.kakao.com https://t1.daumcdn.net http://t1.daumcdn.net https://s1.daumcdn.net http://s1.daumcdn.net https://maps.googleapis.com https://maps.gstatic.com https://cdn.jsdelivr.net; " +
               "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
               "font-src 'self' data: https://fonts.gstatic.com; " +
               "img-src 'self' data: " + s3Origin + cdnOrigin + " http://t1.daumcdn.net https://t1.daumcdn.net http://s1.daumcdn.net https://s1.daumcdn.net http://mts.daumcdn.net https://mts.daumcdn.net http://img1.kakaocdn.net http://t1.kakaocdn.net http://k.kakaocdn.net https://i.ytimg.com https://maps.gstatic.com https://maps.googleapis.com; " +
               "connect-src 'self' https://maps.googleapis.com https://maps.gstatic.com https://dapi.kakao.com http://dapi.kakao.com https://apis.map.kakao.com http://apis.map.kakao.com; " +
               // base-uri: <base> 태그 인젝션으로 상대 경로를 외부로 납치하는 공격 차단
               "base-uri 'self'; " +
               // form-action: 폼 제출 대상을 동일 출처로 한정 (피싱 사이트로의 데이터 탈취 방지)
               "form-action 'self'; " +
               // frame-ancestors: X-Frame-Options: DENY와 이중으로 클릭재킹 차단
               "frame-ancestors 'none';";
    }
}
