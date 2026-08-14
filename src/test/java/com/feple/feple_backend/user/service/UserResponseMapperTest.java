package com.feple.feple_backend.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.entity.AuthProvider;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.UserRole;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserResponseMapperTest {

    @Mock FileStorageService fileStorageService;

    private User.UserBuilder baseUser() {
        return User.builder().id(1L).nickname("닉네임").role(UserRole.USER).bio("소개");
    }

    @Test
    void toUserDto_기본필드_매핑() {
        User user = baseUser().build();

        UserResponseDto dto = UserResponseMapper.toUserDto(user, fileStorageService);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getNickname()).isEqualTo("닉네임");
        assertThat(dto.getRole()).isEqualTo(UserRole.USER);
        assertThat(dto.getBio()).isEqualTo("소개");
    }

    // 절대 URL 통과/기본 로고 제외/상대경로 buildUrl 위임 로직 자체는
    // FileStorageService.resolveProfileImageUrl로 옮겨져 FileStorageServiceTest에서 검증한다 —
    // 여기서는 매퍼가 그 결과를 그대로 필드에 위임하는지만 확인한다.
    @Test
    void toUserDto_프로필이미지는_fileStorageService_resolveProfileImageUrl_결과를_그대로_사용() {
        User user = baseUser().profileImageUrl("user-profiles/1/a.jpg").build();
        given(fileStorageService.resolveProfileImageUrl("user-profiles/1/a.jpg"))
                .willReturn("https://cdn.example.com/user-profiles/1/a.jpg");

        UserResponseDto dto = UserResponseMapper.toUserDto(user, fileStorageService);

        assertThat(dto.getProfileImageUrl()).isEqualTo("https://cdn.example.com/user-profiles/1/a.jpg");
    }

    @Test
    void toUserDto_프로필이미지가_resolveProfileImageUrl에서_null이면_null() {
        User user = baseUser().profileImageUrl(null).build();
        given(fileStorageService.resolveProfileImageUrl(null)).willReturn(null);

        UserResponseDto dto = UserResponseMapper.toUserDto(user, fileStorageService);

        assertThat(dto.getProfileImageUrl()).isNull();
    }

    @Test
    void toAdminUserDto_관리자_전용_필드_포함() {
        LocalDateTime now = LocalDateTime.now();
        User user = baseUser()
                .email("test@test.com")
                .provider(AuthProvider.KAKAO)
                .point(100)
                .createdAt(now)
                .banReason("사유")
                .bannedBy("admin1")
                .build();

        UserResponseDto dto = UserResponseMapper.toAdminUserDto(user, fileStorageService);

        assertThat(dto.getEmail()).isEqualTo("test@test.com");
        assertThat(dto.getProvider()).isEqualTo(AuthProvider.KAKAO);
        assertThat(dto.getPoint()).isEqualTo(100);
        assertThat(dto.getCreatedAt()).isEqualTo(now);
        assertThat(dto.getBanReason()).isEqualTo("사유");
        assertThat(dto.getBannedBy()).isEqualTo("admin1");
    }
}
