package com.feple.feple_backend.service;

import com.feple.feple_backend.domain.user.AuthProvider;
import com.feple.feple_backend.domain.user.User;
import com.feple.feple_backend.dto.user.KakaoUserResponse;
import com.feple.feple_backend.dto.user.UserRequestDto;
import com.feple.feple_backend.dto.user.UserResponseDto;
import com.feple.feple_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User registerOrLogin(KakaoUserResponse kakaoUser) {
        // kakao_account 가 없으면 예외
        var account = Optional.ofNullable(kakaoUser.getKakaoAccount())
                .orElseThrow(() -> new IllegalArgumentException("카카오 계정 정보가 없습니다."));

        String oauthId = kakaoUser.getId().toString();
        //.orElseThrow(() -> new IllegalArgumentException("이메일 정보가 없습니다."));
        String email   = account.getEmail();
//        String nickname = Optional.ofNullable(account.getProfile())
//                .map(KakaoUserResponse.KakaoAccount.Profile::getNickname)
//                .orElse("KakaoUser");

        return userRepository.findByOauthId(oauthId)
                .orElseGet(() -> userRepository.save(User.builder()
                        .oauthId(oauthId)
                        .email(email)
                        //.nickname(nickname)
                        .provider(AuthProvider.KAKAO)
                        .profileImageUrl(account.getProfile().getProfile_image_url())
                        .build()
                ));
    }

    public Long createUser(UserRequestDto dto) {
        User user = User.builder()
                .email(dto.getEmail())
                .nickname(dto.getNickname())
                .oauthId(dto.getOauthId())
                .provider(dto.getProvider())
                .profileImageUrl(dto.getProfileImageUrl())
                .build();
        return userRepository.save(user).getId();
    }

    public UserResponseDto getUser(Long id) {
        return userRepository.findById(id)
                .map(UserResponseDto::from)
                .orElseThrow(() -> new RuntimeException("사용자 없음"));
    }

    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponseDto::from)
                .collect(Collectors.toList());
    }

    public UserResponseDto updateNickname(Long id, String nickname) {
        User user = userRepository.findById(id)
                .orElseThrow(()->
                        new NoSuchElementException("해당 사용자를 찾을 수 없습니다. id="+id)
                );
        user.changeNickname(nickname);
        return UserResponseDto.from(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

}
