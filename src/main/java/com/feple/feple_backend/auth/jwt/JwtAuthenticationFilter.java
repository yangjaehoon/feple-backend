package com.feple.feple_backend.auth.jwt;

import com.feple.feple_backend.global.exception.ErrorCode;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import com.feple.feple_backend.user.service.UserAccessTrackingService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    /** 인증 실패 사유를 request attribute로 전달해, SecurityConfig의 AuthenticationEntryPoint가
     *  (보호된 엔드포인트에 한해) 구체적인 오류 메시지를 응답할 수 있게 한다. */
    public static final String JWT_FAILURE_ATTRIBUTE = "jwtAuthFailure";

    public record JwtFailure(HttpStatus status, String message, ErrorCode code) {}

    private final JwtProvider jwtProvider;
    // SecurityConfig에서 @Lazy 프록시로 주입돼 넘어온다 — 필터 빈이 (FilterRegistrationBean 처리 중)
    // 일찍 생성돼도 실제 해석은 첫 요청 처리 시점으로 미뤄져 JPA 기동 순서 문제를 피한다.
    private final UserRepository userRepository;
    private final UserAccessTrackingService userAccessTrackingService;

    public JwtAuthenticationFilter(
            JwtProvider jwtProvider,
            UserRepository userRepository,
            UserAccessTrackingService userAccessTrackingService
    ) {
        this.jwtProvider = jwtProvider;
        this.userRepository = userRepository;
        this.userAccessTrackingService = userAccessTrackingService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/auth/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveBearerToken(request);

        // 토큰이 유효하지 않아도(만료·변조·탈퇴·정지) 요청을 여기서 막지 않는다 — permitAll()로 열린
        // 공개 엔드포인트(피드 조회 등)는 로그인한 사용자의 토큰이 만료된 것만으로 막혀서는 안 되기
        // 때문에, SecurityContext만 비워두고 체인을 계속 진행한다. 인증이 실제로 필요한 엔드포인트는
        // Spring Security의 authorizeHttpRequests가 SecurityContext 없음을 감지해 아래 entry point로
        // 위임하며, 그때 JWT_FAILURE_ATTRIBUTE에 담긴 구체적 사유(만료/변조/정지 등)를 사용한다.
        if (token != null) {
            authenticateUser(token, request).ifPresent(failure -> {
                SecurityContextHolder.clearContext();
                request.setAttribute(JWT_FAILURE_ATTRIBUTE, failure);
            });
        }

        filterChain.doFilter(request, response);
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.startsWith(JwtConstants.BEARER_PREFIX)) {
            return auth.substring(JwtConstants.BEARER_LENGTH);
        }
        return null;
    }

    /** 토큰 파싱 + 사용자 검증. 성공 시 SecurityContext를 설정하고 빈 Optional을 반환하며,
     *  실패 시 SecurityContext는 건드리지 않고 실패 사유를 반환한다. */
    private Optional<JwtFailure> authenticateUser(String token, HttpServletRequest request) {
        Long userId;
        try {
            userId = jwtProvider.parseUserId(token);
        } catch (ExpiredJwtException e) {
            return Optional.of(new JwtFailure(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다.", ErrorCode.TOKEN_EXPIRED));
        } catch (JwtException e) {
            log.warn("[JWT] 변조된 토큰 감지 - URI: {}, reason: {}", request.getRequestURI(), e.getMessage());
            return Optional.of(new JwtFailure(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.", ErrorCode.TOKEN_INVALID));
        } catch (Exception e) {
            log.error("[JWT] 예상하지 못한 토큰 파싱 오류", e);
            return Optional.of(new JwtFailure(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.", ErrorCode.TOKEN_INVALID));
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.isDeleted()) {
            return Optional.of(new JwtFailure(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.", ErrorCode.TOKEN_INVALID));
        }
        if (user.isBanned()) {
            return Optional.of(new JwtFailure(HttpStatus.FORBIDDEN, "계정이 정지되었습니다.", ErrorCode.USER_BANNED));
        }
        // 나이 확인 전(생년월일 미입력) 유저는 GET /users/me 외의 인증 요청을 모두 차단한다 —
        // 글·댓글 작성 등 개인정보를 남기는 행위(인증 필요 엔드포인트)를 클라이언트 우회와
        // 무관하게 서버에서 막는다(App Store 심사 5.1.1). 공개 GET 조회는 게스트와 동일하게 허용.
        if (user.needsAgeVerification() && !isAgeGateSelfCheck(request)) {
            return Optional.of(new JwtFailure(HttpStatus.FORBIDDEN,
                    "생년월일 확인이 필요합니다.", ErrorCode.AGE_VERIFICATION_REQUIRED));
        }

        String role = "ROLE_" + user.getRole().name();
        var authentication = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority(role))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        userAccessTrackingService.recordAccess(userId);
        return Optional.empty();
    }

    /** 나이 확인 전 유저에게도 허용되는 유일한 경로 — 본인 정보 조회(확인 필요 여부 플래그를 읽음). */
    private boolean isAgeGateSelfCheck(HttpServletRequest request) {
        return "GET".equals(request.getMethod()) && "/users/me".equals(request.getRequestURI());
    }
}
