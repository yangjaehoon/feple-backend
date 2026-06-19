package com.feple.feple_backend.auth.jwt;

import com.feple.feple_backend.global.exception.ErrorResponse;
import com.feple.feple_backend.user.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

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

        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (auth != null && auth.startsWith(JwtConstants.BEARER_PREFIX)) {
            String token = auth.substring(JwtConstants.BEARER_LENGTH);
            try {
                Long userId = jwtProvider.parseUserId(token);

                var user = userRepository.findById(userId).orElse(null);
                if (user == null || user.isDeleted()) {
                    writeJson(response, HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.", "TOKEN_INVALID");
                    return;
                }
                if (user.isBanned()) {
                    writeJson(response, HttpStatus.FORBIDDEN, "계정이 정지되었습니다.", "USER_BANNED");
                    return;
                }

                String role = "ROLE_" + user.getRole().name();
                var authentication = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of(new SimpleGrantedAuthority(role))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (ExpiredJwtException e) {
                // 만료된 토큰 — 클라이언트가 /auth/refresh 를 호출하도록 401 응답
                SecurityContextHolder.clearContext();
                writeJson(response, HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다.", "TOKEN_EXPIRED");
                return;
            } catch (JwtException e) {
                log.warn("[JWT] 변조된 토큰 감지 - URI: {}, reason: {}", request.getRequestURI(), e.getMessage());
                SecurityContextHolder.clearContext();
                writeJson(response, HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.", "TOKEN_INVALID");
                return;
            } catch (Exception e) {
                log.error("[JWT] 예상하지 못한 토큰 파싱 오류", e);
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private static void writeJson(HttpServletResponse response, HttpStatus status, String message, String code)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(ErrorResponse.toJson(status, message, code));
    }
}