package com.feple.feple_backend.auth.jwt;

import com.feple.feple_backend.global.exception.ErrorCode;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    /** 인증 실패 사유를 request attribute로 전달해, SecurityConfig의 AuthenticationEntryPoint가
     *  (보호된 엔드포인트에 한해) 구체적인 오류 메시지를 응답할 수 있게 한다. */
    public static final String JWT_FAILURE_ATTRIBUTE = "jwtAuthFailure";

    public record JwtFailure(HttpStatus status, String message, ErrorCode code) {}

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtProvider jwtProvider, UserRepository userRepository) {
        this.jwtProvider = jwtProvider;
        this.userRepository = userRepository;
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

        String role = "ROLE_" + user.getRole().name();
        var authentication = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority(role))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return Optional.empty();
    }
}
