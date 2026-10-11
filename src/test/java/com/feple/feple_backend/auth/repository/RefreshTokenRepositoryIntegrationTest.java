package com.feple.feple_backend.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.feple.feple_backend.auth.entity.RefreshToken;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RefreshTokenRepositoryIntegrationTest {

    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired UserRepository userRepository;
    @PersistenceContext EntityManager em;

    @MockitoBean FileStorageService fileStorageService;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .oauthId("rt-test-user").nickname("토큰유저").build());
    }

    // ── findByTokenHash ──────────────────────────────────────────────

    @Test
    void 토큰_해시로_조회() {
        refreshTokenRepository.save(token("hash-abc", LocalDateTime.now().plusDays(7)));
        em.flush(); em.clear();

        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash("hash-abc");

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(user.getId());
    }

    @Test
    void 없는_해시_조회_시_empty() {
        assertThat(refreshTokenRepository.findByTokenHash("nonexistent")).isEmpty();
    }

    // ── markRotated (@Modifying) ─────────────────────────────────────

    @Test
    void 교체_표시는_아직_교체되지_않은_토큰에만_한번_기록된다() {
        refreshTokenRepository.save(token("rot-hash", LocalDateTime.now().plusDays(7)));
        em.flush();
        LocalDateTime first = LocalDateTime.now().minusSeconds(10);

        refreshTokenRepository.markRotated("rot-hash", first);
        refreshTokenRepository.markRotated("rot-hash", LocalDateTime.now());
        em.clear();

        RefreshToken found = refreshTokenRepository.findByTokenHash("rot-hash").orElseThrow();
        assertThat(found.isRotated()).isTrue();
        assertThat(found.getRotatedAt()).isEqualToIgnoringNanos(first);
    }

    // ── deleteSupersededByUserId / deleteRotatedBefore ───────────────

    @Test
    void 교체시_유예중인_직전_토큰만_남기고_나머지_삭제() {
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.save(token("active", now.plusDays(7)));
        refreshTokenRepository.save(token("in-grace", now.plusDays(7)));
        refreshTokenRepository.save(token("stale", now.plusDays(7)));
        em.flush();
        refreshTokenRepository.markRotated("in-grace", now.minusMinutes(1));
        refreshTokenRepository.markRotated("stale", now.minusMinutes(10));

        refreshTokenRepository.deleteSupersededByUserId(user.getId(), now.minusMinutes(5));
        em.clear();

        assertThat(refreshTokenRepository.findByTokenHash("active")).isEmpty();
        assertThat(refreshTokenRepository.findByTokenHash("in-grace")).isPresent();
        assertThat(refreshTokenRepository.findByTokenHash("stale")).isEmpty();
    }

    @Test
    void 유예가_지난_교체된_토큰만_정리하고_유효한_토큰은_유지() {
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.save(token("live", now.plusDays(7)));
        refreshTokenRepository.save(token("old-rotated", now.plusDays(7)));
        em.flush();
        refreshTokenRepository.markRotated("old-rotated", now.minusMinutes(10));

        refreshTokenRepository.deleteRotatedBefore(now.minusMinutes(5));
        em.clear();

        assertThat(refreshTokenRepository.findByTokenHash("live")).isPresent();
        assertThat(refreshTokenRepository.findByTokenHash("old-rotated")).isEmpty();
    }

    // ── deleteByUserId (@Modifying) ──────────────────────────────────

    @Test
    void userId로_토큰_전체_삭제() {
        refreshTokenRepository.save(token("u-hash-1", LocalDateTime.now().plusDays(7)));
        refreshTokenRepository.save(token("u-hash-2", LocalDateTime.now().plusDays(7)));
        em.flush();

        refreshTokenRepository.deleteByUserId(user.getId());
        em.clear();

        assertThat(refreshTokenRepository.findByTokenHash("u-hash-1")).isEmpty();
        assertThat(refreshTokenRepository.findByTokenHash("u-hash-2")).isEmpty();
    }

    // ── deleteExpiredTokens ──────────────────────────────────────────

    @Test
    void 만료된_토큰만_삭제() {
        refreshTokenRepository.save(token("expired", LocalDateTime.now().minusDays(1)));
        refreshTokenRepository.save(token("valid", LocalDateTime.now().plusDays(7)));
        em.flush();

        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        em.clear();

        assertThat(refreshTokenRepository.findByTokenHash("expired")).isEmpty();
        assertThat(refreshTokenRepository.findByTokenHash("valid")).isPresent();
    }

    @Test
    void 만료_토큰_없으면_정상_토큰_유지() {
        refreshTokenRepository.save(token("v1", LocalDateTime.now().plusDays(1)));
        refreshTokenRepository.save(token("v2", LocalDateTime.now().plusDays(2)));
        em.flush();

        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        em.clear();

        assertThat(refreshTokenRepository.findByTokenHash("v1")).isPresent();
        assertThat(refreshTokenRepository.findByTokenHash("v2")).isPresent();
    }

    // ── 트랜잭션 격리 ────────────────────────────────────────────────

    @Test
    void 토큰_해시_유니크_제약() {
        refreshTokenRepository.save(token("unique-hash", LocalDateTime.now().plusDays(7)));
        em.flush();

        // 같은 해시로 다시 저장 시도
        org.junit.jupiter.api.Assertions.assertThrows(
                Exception.class,
                () -> {
                    refreshTokenRepository.save(token("unique-hash", LocalDateTime.now().plusDays(7)));
                    em.flush();
                }
        );
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────

    private RefreshToken token(String hash, LocalDateTime expiresAt) {
        return RefreshToken.of(user, hash, expiresAt);
    }
}
