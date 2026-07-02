package com.feple.feple_backend.global;

import com.feple.feple_backend.post.entity.Resolvable;
import com.feple.feple_backend.post.entity.ReportStatus;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;

public class ReportDismissHelper {

    private ReportDismissHelper() {}

    public static <T extends Resolvable> void dismiss(
            ListCrudRepository<T, Long> repo, Long reportId) {
        T report = EntityFinder.getOrThrow(repo::findById, reportId, "신고");
        report.resolve(ReportStatus.DISMISSED);
    }

    public static <T extends Resolvable> void bulkDismiss(
            ListCrudRepository<T, Long> repo, List<Long> ids) {
        if (ids.isEmpty()) return;
        repo.findAllById(ids).stream()
                .filter(Resolvable::isPending)
                .forEach(r -> r.resolve(ReportStatus.DISMISSED));
    }
}
