package com.feple.feple_backend.user.service;

import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface UserService {
    Map<String, Object> checkNicknameAvailable(String nickname, Long excludeUserId);
    UserResponseDto getUser(Long id);
    UserResponseDto getAdminUser(Long id);
    UserResponseDto updateNickname(Long id, String nickname);
    UserResponseDto updateProfileImage(Long id, MultipartFile file) throws IOException;
    void deleteUser(Long id);
    Page<UserResponseDto> getUsersPage(int page, int size, String keyword);
    void bulkDeleteUsers(List<Long> ids);
    void adminDeleteUser(Long id);
    UserResponseDto toUserDto(User user);
    UserResponseDto toAdminUserDto(User user);
    void updateUserRole(Long userId, UserRole role);
    Long currentUserId();
}
