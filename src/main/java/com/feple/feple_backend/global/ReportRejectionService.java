package com.feple.feple_backend.global;

import com.feple.feple_backend.global.entity.ReportStatus;
import com.feple.feple_backend.global.entity.ResolvableReport;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

public class ReportRejectionService {

    private ReportRejectionService() {}

    // 호출부가 postId/commentId 등 후속 처리를 위해 신고 대상을 다시 조회하지 않도록
    // 반려 처리한 엔티티를 그대로 반환한다.
    public static <T extends ResolvableReport> T reject(
            ListCrudRepository<T, Long> repo, Long reportId) {
        T report = EntityLoader.getOrThrow(repo::findById, reportId, "신고");
        // bulkDismiss()와 동일하게 이미 처리된 신고의 재처리를 막는다 (일관성 + 이후 resolve()에
        // 부수효과가 추가되더라도 이중 클릭·요청 재시도로부터 안전하도록 방어)
        if (!report.isPending()) {
            throw new IllegalArgumentException("이미 처리된 신고입니다.");
        }
        report.resolve(ReportStatus.REJECTED);
        return report;
    }

    public static <T extends ResolvableReport> List<T> bulkDismiss(
            ListCrudRepository<T, Long> repo, List<Long> ids) {
        if (ids.isEmpty()) return List.of();
        List<T> rejected = repo.findAllById(ids).stream()
                .filter(ResolvableReport::isPending)
                .toList();
        rejected.forEach(r -> r.resolve(ReportStatus.REJECTED));
        return rejected;
    }
}
