package com.feple.feple_backend.user.service;

import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface UserService {
    Map<String, Object> checkNicknameAvailable(String nickname, Long excludeUserId);
    UserResponseDto getUser(Long id);
    void updateNickname(Long id, String nickname);
    void updateProfileImage(Long id, MultipartFile file);
    void updateBio(Long id, String bio);
    void deleteUser(Long id);
    Long currentUserId();
    UserResponseDto toUserDto(User user);
}
