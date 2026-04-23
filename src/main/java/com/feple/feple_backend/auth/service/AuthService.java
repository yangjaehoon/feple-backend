package com.feple.feple_backend.auth.service;

import com.feple.feple_backend.user.entity.AuthProvider;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.auth.dto.KakaoUserResponse;
import com.feple.feple_backend.auth.dto.LocalLoginRequest;
import com.feple.feple_backend.auth.dto.RegisterRequest;
import com.feple.feple_backend.user.repository.UserRepository;
import com.feple.feple_backend.user.NicknameValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User registerOrLogin(KakaoUserResponse kakaoUser) {
        var account = Optional.ofNullable(kakaoUser.getKakaoAccount())
                .orElseThrow(() -> new IllegalArgumentException("카카오 계정 정보가 없습니다."));

        String oauthId = kakaoUser.getId().toString();
        String email = account.getEmail();

        String rawNickname = Optional.ofNullable(account.getProfile())
                .map(KakaoUserResponse.Profile::getNickname)
                .filter(n -> !n.isBlank())
                .orElse("KakaoUser");
        String sanitized = rawNickname.trim().replaceAll("[^가-힣a-zA-Z0-9_]", "");
        String nickname = sanitized.isBlank() ? "KakaoUser" : sanitized;

        Optional<User> existingUser = userRepository.findByOauthId(oauthId);
        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        String kakaoImageUrl = Optional.ofNullable(account.getProfile())
                .map(KakaoUserResponse.Profile::getProfile_image_url)
                .filter(url -> !url.isBlank())
                .orElse(null);

        User newUser = User.builder()
                .oauthId(oauthId)
                .email(email)
                .nickname(nickname)
                .provider(AuthProvider.KAKAO)
                .profileImageUrl(kakaoImageUrl)
                .build();

        return userRepository.save(newUser);
    }

    @Transactional
    public User registerOrLoginFirebase(String uid, String email, String displayName) {
        return userRepository.findByProviderAndOauthId(AuthProvider.FIREBASE, uid)
                .orElseGet(() -> {
                    String raw = (displayName != null && !displayName.isBlank())
                            ? displayName
                            : "User" + uid.substring(0, Math.min(uid.length(), 8));
                    // 허용되지 않는 문자 제거 후 빈 경우 fallback
                    String sanitized = raw.trim().replaceAll("[^가-힣a-zA-Z0-9_]", "");
                    String base = sanitized.isBlank() ? "User" + uid.substring(0, Math.min(uid.length(), 8)) : sanitized;
                    String nickname = uniqueNickname(base);
                    User user = User.builder()
                            .email(email)
                            .nickname(nickname)
                            .oauthId(uid)
                            .provider(AuthProvider.FIREBASE)
                            .build();
                    return userRepository.save(user);
                });
    }

    @Transactional
    public User registerLocal(RegisterRequest req) {
        if (userRepository.findByProviderAndOauthId(AuthProvider.EMAIL, req.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
        NicknameValidator.validate(req.getNickname());
        if (userRepository.existsByNickname(req.getNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }
        User user = User.builder()
                .email(req.getEmail())
                .nickname(req.getNickname().trim())
                .oauthId(req.getEmail())
                .provider(AuthProvider.EMAIL)
                .password(passwordEncoder.encode(req.getPassword()))
                .profileImageUrl(null)
                .build();
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User loginLocal(LocalLoginRequest req) {
        User user = userRepository.findByProviderAndOauthId(AuthProvider.EMAIL, req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        return user;
    }

    private String uniqueNickname(String base) {
        if (base.length() > 8)
            base = base.substring(0, 8);
        if (!userRepository.existsByNickname(base))
            return base;
        for (int i = 2; i <= 999; i++) {
            String candidate = base.substring(0, Math.min(base.length(), 6)) + i;
            if (!userRepository.existsByNickname(candidate))
                return candidate;
        }
        return base + System.currentTimeMillis() % 10000;
    }
}
