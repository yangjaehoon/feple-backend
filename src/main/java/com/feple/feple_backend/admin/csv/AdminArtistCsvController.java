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
@RequiresAdminPermission(AdminPermission.ARTISTS)
@Controller
@RequestMapping("/admin/export")
@RequiredArgsConstructor
public class AdminArtistCsvController {

    private final ArtistCsvExporter artistCsvExporter;
    private final AdminLogService adminLogService;

    @GetMapping("/artists.csv")
    @ResponseBody
    public ResponseEntity<byte[]> exportArtists() {
        return CsvExporter.export(adminLogService, AdminAction.EXPORT_ARTISTS, "ARTIST", "artists", artistCsvExporter::buildCsv);
    }
}
