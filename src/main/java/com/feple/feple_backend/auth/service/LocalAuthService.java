package com.feple.feple_backend.auth.service;

import com.feple.feple_backend.auth.dto.LocalLoginRequest;
import com.feple.feple_backend.auth.dto.RegisterRequest;
import com.feple.feple_backend.user.NicknameValidator;
import com.feple.feple_backend.user.entity.AuthProvider;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class LocalAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User register(RegisterRequest req) {
        if (userRepository.findByProviderAndOauthId(AuthProvider.EMAIL, req.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
        NicknameValidator.validate(req.getNickname());
        if (userRepository.existsByNickname(req.getNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }
        return userRepository.save(User.builder()
                .email(req.getEmail())
                .nickname(req.getNickname().trim())
                .oauthId(req.getEmail())
                .provider(AuthProvider.EMAIL)
                .password(passwordEncoder.encode(req.getPassword()))
                .profileImageUrl(null)
                .build());
    }

    @Transactional(readOnly = true)
    public User login(LocalLoginRequest req) {
        User user = userRepository.findByProviderAndOauthId(AuthProvider.EMAIL, req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        return user;
    }
}
