package com.feple.feple_backend.admin.csv;

import com.feple.feple_backend.admin.account.AdminPermission;
import com.feple.feple_backend.admin.account.RequiresAdminPermission;
import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@PreAuthorize("hasRole('ADMIN')")
@RequiresAdminPermission(value = AdminPermission.FESTIVALS, writeOnly = true)
@Controller
@RequestMapping("/admin/export")
@RequiredArgsConstructor
public class AdminFestivalCsvController {

    private final FestivalCsvExporter festivalCsvExporter;
    private final AdminLogService adminLogService;

    @GetMapping("/festivals.csv")
    @ResponseBody
    public ResponseEntity<byte[]> exportFestivals() {
        return CsvExporter.export(adminLogService, AdminAction.EXPORT_FESTIVALS, "FESTIVAL", "festivals", festivalCsvExporter::buildCsv);
    }
}
