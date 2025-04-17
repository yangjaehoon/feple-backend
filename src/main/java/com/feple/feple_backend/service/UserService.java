package com.feple.feple_backend.service;

import com.feple.feple_backend.domain.user.User;
import com.feple.feple_backend.dto.user.UserRequestDto;
import com.feple.feple_backend.dto.user.UserResponseDto;
import com.feple.feple_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

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

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

}
