package com.feple.feple_backend.admin.moderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.entity.UserRole;
import com.feple.feple_backend.user.service.UserAdminService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserCsvExporterTest {

    @Mock UserAdminService userAdminService;

    @InjectMocks UserCsvExporter exporter;

    private UserResponseDto.UserResponseDtoBuilder baseUser() {
        return UserResponseDto.builder()
                .id(1L).nickname("닉네임").email("test@test.com")
                .role(UserRole.USER).createdAt(LocalDateTime.of(2026, 1, 1, 0, 0, 0));
    }

    @Test
    void buildCsv_유저없으면_헤더만_반환() {
        given(userAdminService.getAllUsersForExport()).willReturn(List.of());

        assertThat(exporter.buildCsv()).isEqualTo("ID,닉네임,이메일,역할,가입일,정지여부\n");
    }

    @Test
    void buildCsv_정상유저는_정지여부_빈값() {
        given(userAdminService.getAllUsersForExport()).willReturn(List.of(baseUser().build()));

        String csv = exporter.buildCsv();

        assertThat(csv).contains("1,닉네임,test@test.com,일반 사용자,2026-01-01 00:00:00,\n");
    }

    @Test
    void buildCsv_영구정지_유저는_영구정지_표시() {
        UserResponseDto user = baseUser().bannedUntil(LocalDateTime.of(9999, 12, 31, 0, 0, 0)).build();
        given(userAdminService.getAllUsersForExport()).willReturn(List.of(user));

        assertThat(exporter.buildCsv()).contains(",영구정지\n");
    }

    @Test
    void buildCsv_기간정지_유저는_정지중_표시() {
        UserResponseDto user = baseUser().bannedUntil(LocalDateTime.now().plusDays(7)).build();
        given(userAdminService.getAllUsersForExport()).willReturn(List.of(user));

        assertThat(exporter.buildCsv()).contains(",정지중\n");
    }
}
