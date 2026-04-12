package com.feple.feple_backend.auth.service;

import com.feple.feple_backend.user.domain.AuthProvider;
import com.feple.feple_backend.user.domain.User;
import com.feple.feple_backend.user.dto.KakaoUserResponse;
import com.feple.feple_backend.auth.dto.LocalLoginRequest;
import com.feple.feple_backend.auth.dto.RegisterRequest;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[가-힣a-zA-Z0-9_]+$");

    public User registerOrLogin(KakaoUserResponse kakaoUser) {
        var account = Optional.ofNullable(kakaoUser.getKakaoAccount())
                .orElseThrow(() -> new IllegalArgumentException("카카오 계정 정보가 없습니다."));

        String oauthId = kakaoUser.getId().toString();
        String email = account.getEmail();

        String nickname = Optional.ofNullable(account.getProfile())
                .map(KakaoUserResponse.Profile::getNickname)
                .filter(n -> !n.isBlank())
                .orElse("KakaoUser");

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
                    String base = (displayName != null && !displayName.isBlank())
                            ? displayName
                            : "User" + uid.substring(0, Math.min(uid.length(), 8));
                    String nickname = uniqueNickname(base.trim());
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
            throw new IllegalArgumentException("���미 가입된 이메일입니다.");
        }
        validateNickname(req.getNickname());
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

    private void validateNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("닉네임을 입력해주세요.");
        }
        String trimmed = nickname.trim();
        if (trimmed.length() < 2 || trimmed.length() > 8) {
            throw new IllegalArgumentException("닉네임은 2자 이상 8자 이하로 입력해주세요.");
        }
        if (!NICKNAME_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("닉네임은 한글, 영문, 숫자, 밑줄(_)만 사용할 수 있습니다.");
        }
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
