package com.feple.feple_backend.global;

import com.feple.feple_backend.post.entity.ResolvableReport;
import com.feple.feple_backend.post.entity.ReportStatus;
import org.springframework.data.repository.ListCrudRepository;

import java.util.List;

public class ReportRejectionService {

    private ReportRejectionService() {}

    public static <T extends ResolvableReport> void reject(
            ListCrudRepository<T, Long> repo, Long reportId) {
        T report = EntityLoader.getOrThrow(repo::findById, reportId, "신고");
        // bulkDismiss()와 동일하게 이미 처리된 신고의 재처리를 막는다 (일관성 + 이후 resolve()에
        // 부수효과가 추가되더라도 이중 클릭·요청 재시도로부터 안전하도록 방어)
        if (!report.isPending()) {
            throw new IllegalArgumentException("이미 처리된 신고입니다.");
        }
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
