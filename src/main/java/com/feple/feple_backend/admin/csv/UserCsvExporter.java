package com.feple.feple_backend.admin.csv;

import com.feple.feple_backend.user.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserCsvExporter {

    private final UserAdminService userAdminService;

    public String buildCsv() {
        return CsvExporter.buildCsv("ID,닉네임,이메일,역할,가입일,정지여부\n",
                userAdminService.getAllUsersForExport(),
                u -> new Object[]{
                        u.getId(), u.getNickname(), u.getEmail(), u.getRoleDisplayName(),
                        CsvExporter.formatDt(u.getCreatedAt()),
                        u.isBanned() ? (u.isPermanentBan() ? "영구정지" : "정지중") : "" });
    }
}
