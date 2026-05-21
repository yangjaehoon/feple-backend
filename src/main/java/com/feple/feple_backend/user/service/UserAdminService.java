package com.feple.feple_backend.user.service;

import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.entity.UserRole;
import org.springframework.data.domain.Page;

import java.util.List;

public interface UserAdminService {
    UserResponseDto getAdminUser(Long id);
    Page<UserResponseDto> getUsersPage(int page, int size, String keyword);
    Page<UserResponseDto> getUsersPageSortedByReports(int page, int size, String keyword);
    void bulkDeleteUsers(List<Long> ids);
    void adminDeleteUser(Long id);
    void updateUserRole(Long userId, UserRole role);
}
