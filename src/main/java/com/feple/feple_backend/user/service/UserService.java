package com.feple.feple_backend.user.service;

import com.feple.feple_backend.user.dto.NicknameAvailabilityResponse;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.WithdrawalReason;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    NicknameAvailabilityResponse checkNicknameAvailable(String nickname, Long excludeUserId);
    UserResponseDto getUser(Long id);
    void updateNickname(Long id, String nickname);
    void updateProfileImage(Long id, MultipartFile file);
    void updateBio(Long id, String bio);
    void deleteUser(Long id, WithdrawalReason reason, String detail);
    Long currentUserId();
    UserResponseDto toUserDto(User user);
}
