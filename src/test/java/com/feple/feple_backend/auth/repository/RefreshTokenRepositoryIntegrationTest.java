package com.feple.feple_backend.auth.repository;

import com.feple.feple_backend.auth.entity.RefreshToken;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

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

    // ── deleteByTokenHash (@Modifying) ───────────────────────────────

    @Test
    void 토큰_해시로_삭제() {
        refreshTokenRepository.save(token("del-hash", LocalDateTime.now().plusDays(7)));
        em.flush();

        int deleted = refreshTokenRepository.deleteByTokenHash("del-hash");
        em.clear();

        assertThat(deleted).isEqualTo(1);
        assertThat(refreshTokenRepository.findByTokenHash("del-hash")).isEmpty();
    }

    @Test
    void 없는_해시_삭제_시_0_반환() {
        int deleted = refreshTokenRepository.deleteByTokenHash("no-such-hash");
        assertThat(deleted).isEqualTo(0);
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
