package com.feple.feple_backend.global;

import com.feple.feple_backend.post.entity.ResolvableReport;
import com.feple.feple_backend.post.entity.ReportStatus;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;

public class ReportRejectionService {

    private ReportRejectionService() {}

    public static <T extends ResolvableReport> void dismiss(
            ListCrudRepository<T, Long> repo, Long reportId) {
        T report = EntityRequirer.getOrThrow(repo::findById, reportId, "신고");
        report.resolve(ReportStatus.REJECTED);
    }

    public static <T extends ResolvableReport> void bulkDismiss(
            ListCrudRepository<T, Long> repo, List<Long> ids) {
        if (ids.isEmpty()) return;
        repo.findAllById(ids).stream()
                .filter(ResolvableReport::isPending)
                .forEach(r -> r.resolve(ReportStatus.REJECTED));
    }
}
