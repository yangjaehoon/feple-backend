package com.feple.feple_backend.admin.csv;

import com.feple.feple_backend.admin.account.AdminPermission;
import com.feple.feple_backend.admin.account.RequiresAdminPermission;
import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.global.ReportTypes;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@RequiresAdminPermission(value = AdminPermission.REPORTS, writeOnly = true)
@Controller
@RequestMapping("/admin/export")
public class AdminReportCsvController {

    private final AdminLogService adminLogService;
    private final Map<String, ReportCsvExporter> reportExporters;

    public AdminReportCsvController(AdminLogService adminLogService, List<ReportCsvExporter> exporters) {
        this.adminLogService = adminLogService;
        this.reportExporters = exporters.stream()
                .collect(Collectors.toMap(ReportCsvExporter::getReportType, e -> e));
    }

    @GetMapping("/reports.csv")
    @ResponseBody
    public ResponseEntity<byte[]> exportReports(@RequestParam(defaultValue = ReportTypes.POST) String type) {
        // type이 어느 exporter에도 등록되지 않은 값이면 getOrDefault로 post 쪽에 조용히 fallback하지
        // 않고 400을 반환한다 — 과거에 여기서 photo 타입 exporter가 누락돼 photo 요청이 post 신고
        // 데이터로 조용히 내려가던 버그가 있었다.
        if (!reportExporters.containsKey(type)) {
            return ResponseEntity.badRequest().build();
        }
        ReportCsvExporter exporter = reportExporters.get(type);
        adminLogService.log(AdminAction.EXPORT_REPORTS, "REPORT", null, type);
        return CsvExporter.csvResponse(exporter.buildCsv(), "reports_" + type + "_" + LocalDate.now() + ".csv");
    }
}
