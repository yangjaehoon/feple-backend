package com.feple.feple_backend.auth.service;

import com.feple.feple_backend.auth.entity.RefreshToken;
import com.feple.feple_backend.auth.jwt.JwtProperties;
import com.feple.feple_backend.auth.repository.RefreshTokenRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.UserRole;
import com.feple.feple_backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock UserRepository userRepository;

    private RefreshTokenService refreshTokenService;

    // 24시간 만료, 레코드 직접 생성으로 mock 불필요
    private static final JwtProperties JWT_PROPS =
            new JwtProperties("test-secret-key-long-enough", 3_600_000L, 86_400_000L);

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, JWT_PROPS, userRepository);
    }

    private User user(Long id) {
        return User.builder().id(id).nickname("user" + id)
                .oauthId("o" + id).role(UserRole.USER).build();
    }

    private RefreshToken validToken(User user, String rawToken) {
        String hash = refreshTokenService.hash(rawToken);
        return RefreshToken.of(user, hash, LocalDateTime.now().plusDays(1));
    }

    private RefreshToken expiredToken(User user, String rawToken) {
        String hash = refreshTokenService.hash(rawToken);
        return RefreshToken.of(user, hash, LocalDateTime.now().minusHours(1));
    }

    // ── hash ─────────────────────────────────────────────────────────

    @Test
    void 동일_입력이면_동일_해시_반환() {
        String h1 = refreshTokenService.hash("token-abc");
        String h2 = refreshTokenService.hash("token-abc");

        assertThat(h1).isEqualTo(h2);
    }

    @Test
    void 다른_입력이면_다른_해시_반환() {
        String h1 = refreshTokenService.hash("token-abc");
        String h2 = refreshTokenService.hash("token-xyz");

        assertThat(h1).isNotEqualTo(h2);
    }

    @Test
    void 해시는_64자_16진수_문자열() {
        String hash = refreshTokenService.hash("any-token");

        assertThat(hash).hasSize(64).matches("[0-9a-f]+");
    }

    // ── save ─────────────────────────────────────────────────────────

    @Test
    void 저장시_기존_토큰_삭제_후_새_토큰_저장() {
        User user = user(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        refreshTokenService.save(1L, "raw-token");

        verify(refreshTokenRepository).deleteByUserId(1L);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void 존재하지_않는_사용자로_저장시_예외() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.save(99L, "raw-token"))
                .isInstanceOf(IllegalStateException.class);
    }

    // ── validateAndConsume ───────────────────────────────────────────

    @Test
    void 유효한_토큰_소비시_userId_반환() {
        User user = user(1L);
        String raw = "valid-token";
        RefreshToken token = validToken(user, raw);
        given(refreshTokenRepository.findByTokenHash(refreshTokenService.hash(raw)))
                .willReturn(Optional.of(token));
        given(refreshTokenRepository.deleteByTokenHash(refreshTokenService.hash(raw))).willReturn(1);

        Long result = refreshTokenService.validateAndConsume(raw);

        assertThat(result).isEqualTo(1L);
    }

    @Test
    void 존재하지_않는_토큰_소비시_예외() {
        given(refreshTokenRepository.findByTokenHash(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.validateAndConsume("unknown-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않은");
    }

    @Test
    void 만료된_토큰_소비시_deleteByTokenHash_호출_후_예외() {
        User user = user(1L);
        String raw = "expired-token";
        RefreshToken token = expiredToken(user, raw);
        given(refreshTokenRepository.findByTokenHash(refreshTokenService.hash(raw)))
                .willReturn(Optional.of(token));

        assertThatThrownBy(() -> refreshTokenService.validateAndConsume(raw))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("만료");

        verify(refreshTokenRepository).deleteByTokenHash(refreshTokenService.hash(raw));
    }

    @Test
    void 동시_요청으로_이미_삭제된_토큰이면_예외() {
        User user = user(1L);
        String raw = "race-token";
        RefreshToken token = validToken(user, raw);
        given(refreshTokenRepository.findByTokenHash(refreshTokenService.hash(raw)))
                .willReturn(Optional.of(token));
        given(refreshTokenRepository.deleteByTokenHash(refreshTokenService.hash(raw))).willReturn(0);

        assertThatThrownBy(() -> refreshTokenService.validateAndConsume(raw))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않은");
    }

    // ── revoke ───────────────────────────────────────────────────────

    @Test
    void 토큰_revoke시_deleteByTokenHash_호출() {
        String raw = "revoke-token";

        refreshTokenService.revoke(raw);

        verify(refreshTokenRepository).deleteByTokenHash(refreshTokenService.hash(raw));
    }

    @Test
    void 존재하지_않는_토큰_revoke시_예외_없이_무시() {
        // deleteByTokenHash는 존재 여부와 관계없이 항상 호출됨 (0 반환 시 무시)
        refreshTokenService.revoke("unknown-token");

        verify(refreshTokenRepository).deleteByTokenHash(any());
    }

    // ── revokeAll / cleanExpiredTokens ───────────────────────────────

    @Test
    void revokeAll_호출시_deleteByUserId_실행() {
        refreshTokenService.revokeAll(1L);

        verify(refreshTokenRepository).deleteByUserId(1L);
    }

    @Test
    void cleanExpiredTokens_호출시_deleteExpiredTokens_실행() {
        refreshTokenService.cleanExpiredTokens();

        verify(refreshTokenRepository).deleteExpiredTokens(any(LocalDateTime.class));
    }
}
