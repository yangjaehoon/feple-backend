package com.feple.feple_backend.user.service;

import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

public interface UserService {
    Map<String, Object> checkNicknameAvailable(String nickname, Long excludeUserId);
    UserResponseDto getUser(Long id);
    UserResponseDto updateNickname(Long id, String nickname);
    UserResponseDto updateProfileImage(Long id, MultipartFile file) throws IOException;
    void deleteUser(Long id);
    Long currentUserId();
    UserResponseDto toUserDto(User user);
}
