package com.feple.feple_backend.user.repository;

import com.feple.feple_backend.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.security.AuthProvider;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByOauthId(String oauthId);

    Optional<User> findByProviderAndOauthId(AuthProvider provider, String oauthId);

}
