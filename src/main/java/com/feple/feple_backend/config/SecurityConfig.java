package com.feple.feple_backend.config;

import com.feple.feple_backend.admin.AdminLoginFailureHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
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
import com.feple.feple_backend.auth.jwt.JwtAuthenticationFilter;
import com.feple.feple_backend.auth.jwt.JwtProvider;
import com.feple.feple_backend.user.repository.UserRepository;

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

    @Value("${app.cors.allowed-origins:http://localhost:8080}")
    private String allowedOrigins;

    @Value("${app.s3.bucket:}")
    private String s3Bucket;

    // ── 1. 관리자 페이지용 FilterChain (세션 기반 폼 로그인, CSRF 활성화) ──
    @Bean
    @Order(1)
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
                .maximumSessions(1))
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/admin/access-denied"));

        return http.build();
    }

    // ── 2. API용 FilterChain (JWT Stateless) ──
    @Bean
    @Order(2)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
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
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/users/check-nickname").permitAll()
                .requestMatchers("/favicon.ico", "/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/posts/my/scrapped").authenticated()
                .requestMatchers(HttpMethod.GET, "/festivals/**", "/artists/**",
                    "/posts/**", "/comments/**").permitAll()
                .anyRequest().authenticated())
            .httpBasic(hb -> hb.disable())
            .formLogin(fl -> fl.disable())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) ->
                    res.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")))
            .addFilterBefore(new JwtAuthenticationFilter(jwtProvider, userRepository),
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
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
        return "default-src 'self'; " +
               "script-src 'self' 'unsafe-inline' https://dapi.kakao.com https://t1.daumcdn.net http://t1.daumcdn.net https://s1.daumcdn.net http://s1.daumcdn.net https://maps.googleapis.com https://maps.gstatic.com https://cdn.jsdelivr.net; " +
               "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
               "font-src 'self' data: https://fonts.gstatic.com; " +
               "img-src 'self' data: " + s3Origin + " http://t1.daumcdn.net https://t1.daumcdn.net http://s1.daumcdn.net https://s1.daumcdn.net http://mts.daumcdn.net https://mts.daumcdn.net http://img1.kakaocdn.net http://t1.kakaocdn.net http://k.kakaocdn.net https://i.ytimg.com https://maps.gstatic.com https://maps.googleapis.com; " +
               "connect-src 'self' https://maps.googleapis.com https://maps.gstatic.com https://dapi.kakao.com https://apis.map.kakao.com http://apis.map.kakao.com; " +
               // base-uri: <base> 태그 인젝션으로 상대 경로를 외부로 납치하는 공격 차단
               "base-uri 'self'; " +
               // form-action: 폼 제출 대상을 동일 출처로 한정 (피싱 사이트로의 데이터 탈취 방지)
               "form-action 'self'; " +
               // frame-ancestors: X-Frame-Options: DENY와 이중으로 클릭재킹 차단
               "frame-ancestors 'none';";
    }
}
