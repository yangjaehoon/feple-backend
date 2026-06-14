package com.feple.feple_backend.user.repository;

import com.feple.feple_backend.user.entity.UserDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserDeviceTokenRepository extends JpaRepository<UserDeviceToken, Long> {

    @Query("SELECT t FROM UserDeviceToken t WHERE t.user.id = :userId AND t.token = :token")
    Optional<UserDeviceToken> findByUserIdAndToken(@Param("userId") Long userId, @Param("token") String token);

    @Query("SELECT t FROM UserDeviceToken t WHERE t.user.id = :userId")
    List<UserDeviceToken> findByUserId(@Param("userId") Long userId);

    /** 특정 유저 목록의 모든 토큰 조회 (FCM multicast용) */
    @Query("SELECT t.token FROM UserDeviceToken t WHERE t.user.id IN :userIds")
    List<String> findTokensByUserIds(@Param("userIds") List<Long> userIds);

    @Modifying
    @Query("DELETE FROM UserDeviceToken t WHERE t.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM UserDeviceToken t WHERE t.user.id = :userId AND t.token = :token")
    void deleteByUserIdAndToken(@Param("userId") Long userId, @Param("token") String token);

    @Modifying
    @Query("DELETE FROM UserDeviceToken t WHERE t.token = :token AND t.user.id != :userId")
    void deleteByTokenAndOtherUsers(@Param("token") String token, @Param("userId") Long userId);

    @Query("SELECT DISTINCT t.token FROM UserDeviceToken t")
    List<String> findAllTokens();

    @Modifying
    @Query("DELETE FROM UserDeviceToken t WHERE t.token IN :tokens")
    void deleteByTokenIn(@Param("tokens") List<String> tokens);

    @Query("SELECT COUNT(DISTINCT t.user.id) FROM UserDeviceToken t")
    long countDistinctUsers();
}
