package com.feple.feple_backend.user.repository;

import com.feple.feple_backend.user.entity.UserDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserDeviceTokenRepository extends JpaRepository<UserDeviceToken, Long> {

    Optional<UserDeviceToken> findByUserIdAndToken(Long userId, String token);

    List<UserDeviceToken> findByUserId(Long userId);

    /** 특정 유저 목록의 모든 토큰 조회 (FCM multicast용) */
    @Query("SELECT t.token FROM UserDeviceToken t WHERE t.user.id IN :userIds")
    List<String> findTokensByUserIds(@Param("userIds") List<Long> userIds);

    void deleteByUserIdAndToken(Long userId, String token);
}
