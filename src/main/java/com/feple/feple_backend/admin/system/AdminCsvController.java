package com.feple.feple_backend.admin.system;

import com.feple.feple_backend.admin.AdminConstants;
import com.feple.feple_backend.admin.moderation.CsvExporter;
import com.feple.feple_backend.admin.moderation.ReportCsvExporter;
import com.feple.feple_backend.admin.moderation.UserCsvExporter;
import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import lombok.RequiredArgsConstructor;
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

    private final UserCsvExporter userCsvExporter;
    private final AdminLogService adminLogService;
    private final Map<String, ReportCsvExporter> reportExporters;

    public AdminCsvController(UserCsvExporter userCsvExporter,
                               AdminLogService adminLogService,
                               List<ReportCsvExporter> exporters) {
        this.userCsvExporter = userCsvExporter;
        this.adminLogService  = adminLogService;
        this.reportExporters  = exporters.stream()
                .collect(Collectors.toMap(ReportCsvExporter::getReportType, e -> e));
    }

    @GetMapping("/users.csv")
    @ResponseBody
    public ResponseEntity<byte[]> exportUsers() {
        String csv = userCsvExporter.buildCsv();
        adminLogService.log(AdminAction.EXPORT_USERS, "USER", null, "CSV 내보내기");
        return CsvExporter.csvResponse(csv, "users_" + LocalDate.now() + ".csv");
    }

    @GetMapping("/reports.csv")
    @ResponseBody
    public ResponseEntity<byte[]> exportReports(@RequestParam(defaultValue = AdminConstants.REPORT_TYPE_POST) String type) {
        ReportCsvExporter exporter = reportExporters.getOrDefault(type, reportExporters.get(AdminConstants.REPORT_TYPE_POST));
        if (exporter == null) {
            return ResponseEntity.badRequest().build();
        }
        adminLogService.log(AdminAction.EXPORT_REPORTS, "REPORT", null, type);
        return CsvExporter.csvResponse(exporter.buildCsv(), "reports_" + type + "_" + LocalDate.now() + ".csv");
    }
}
