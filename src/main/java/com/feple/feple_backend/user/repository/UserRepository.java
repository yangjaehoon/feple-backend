package com.feple.feple_backend.user.repository;

import com.feple.feple_backend.user.domain.AuthProvider;
import com.feple.feple_backend.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByOauthId(String oauthId);

    Optional<User> findByProviderAndOauthId(AuthProvider provider, String oauthId);

    org.springframework.data.domain.Page<User> findByNicknameContainingIgnoreCaseOrEmailContainingIgnoreCase(String nickname, String email, org.springframework.data.domain.Pageable pageable);

    java.util.List<User> findTop5ByOrderByIdDesc();

    boolean existsByNickname(String nickname);

    boolean existsByNicknameAndIdNot(String nickname, Long id);
}
