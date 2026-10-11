package com.feple.feple_backend.auth.repository;

import com.feple.feple_backend.auth.entity.RefreshToken;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken rt WHERE rt.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    /** 아직 교체되지 않은 토큰만 교체 표시한다 — 동시 요청이 이미 표시했으면 그대로 둔다. */
    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.rotatedAt = :now WHERE rt.tokenHash = :tokenHash AND rt.rotatedAt IS NULL")
    void markRotated(@Param("tokenHash") String tokenHash, @Param("now") LocalDateTime now);

    /**
     * 토큰 교체 시 사용자의 이전 토큰을 정리하되, 유예 시간 안에 교체된 토큰은 남긴다 —
     * 교체 응답을 못 받은 클라이언트가 그 토큰으로 다시 갱신할 수 있어야 한다.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken rt WHERE rt.user.id = :userId"
            + " AND (rt.rotatedAt IS NULL OR rt.rotatedAt < :graceCutoff)")
    void deleteSupersededByUserId(@Param("userId") Long userId,
                                  @Param("graceCutoff") LocalDateTime graceCutoff);

    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken rt WHERE rt.rotatedAt < :graceCutoff")
    void deleteRotatedBefore(@Param("graceCutoff") LocalDateTime graceCutoff);
}
