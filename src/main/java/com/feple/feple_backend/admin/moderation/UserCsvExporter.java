package com.feple.feple_backend.admin.moderation;

import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserCsvExporter {

    private final UserAdminService userAdminService;

    public String buildCsv() {
        StringBuilder sb = new StringBuilder("ID,닉네임,이메일,역할,가입일,정지여부\n");
        for (UserResponseDto u : userAdminService.getAllUsersForExport()) {
            sb.append(CsvExporter.row(
                    u.getId(),
                    u.getNickname(),
                    u.getEmail(),
                    u.getRoleDisplayName(),
                    CsvExporter.formatDt(u.getCreatedAt()),
                    u.isBanned() ? (u.isPermanentBan() ? "영구정지" : "정지중") : ""));
        }
        return sb.toString();
    }
}
