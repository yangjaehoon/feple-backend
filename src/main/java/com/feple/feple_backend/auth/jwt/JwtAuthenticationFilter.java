package com.feple.feple_backend.auth.jwt;

import com.feple.feple_backend.global.exception.ErrorResponse;
import com.feple.feple_backend.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
            } catch (Exception e) {
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