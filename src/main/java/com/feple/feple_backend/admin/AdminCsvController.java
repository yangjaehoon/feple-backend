package com.feple.feple_backend.admin;

import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.service.UserAdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/export")
public class AdminCsvController {

    private final UserAdminService userAdminService;
    private final Map<String, ReportCsvExporter> reportExporters;

    public AdminCsvController(UserAdminService userAdminService, List<ReportCsvExporter> exporters) {
        this.userAdminService = userAdminService;
        this.reportExporters = exporters.stream()
                .collect(Collectors.toMap(ReportCsvExporter::getReportType, e -> e));
    }

    @GetMapping("/users.csv")
    @ResponseBody
    public ResponseEntity<byte[]> exportUsers() {
        List<UserResponseDto> users = userAdminService.getAllUsersForExport();
        StringBuilder sb = new StringBuilder("ID,닉네임,이메일,역할,가입일,정지여부\n");
        for (UserResponseDto u : users) {
            sb.append(CsvExporter.cell(u.getId()))
              .append(',').append(CsvExporter.cell(u.getNickname()))
              .append(',').append(CsvExporter.cell(u.getEmail()))
              .append(',').append(CsvExporter.cell(u.getRole().getDisplayName()))
              .append(',').append(CsvExporter.cell(CsvExporter.formatDt(u.getCreatedAt())))
              .append(',').append(CsvExporter.cell(u.isBanned() ? (u.isPermanentBan() ? "영구정지" : "정지중") : ""))
              .append('\n');
        }
        return CsvExporter.csvResponse(sb.toString(), "users_" + LocalDate.now() + ".csv");
    }

    @GetMapping("/reports.csv")
    @ResponseBody
    public ResponseEntity<byte[]> exportReports(@RequestParam(defaultValue = "post") String type) {
        ReportCsvExporter exporter = reportExporters.getOrDefault(type, reportExporters.get("post"));
        return CsvExporter.csvResponse(exporter.buildCsv(), "reports_" + type + "_" + LocalDate.now() + ".csv");
    }
}
