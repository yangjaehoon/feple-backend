package com.feple.feple_backend.admin.service;

import org.slf4j.LoggerFactory;

import java.util.List;

public interface ReportCommandService {
    void dismissReport(Long reportId);
    void bulkDismiss(List<Long> ids);

    /**
     * 신고된 콘텐츠를 제거하고 해당 신고를 처리 완료 상태로 만든다.
     * 콘텐츠 보존 정책은 구현체마다 의도적으로 다르다 — "신고 레코드가 항상
     * 남는다"는 걸 계약이 보장하지 않으므로 호출부에서 구현체별 차이를
     * 가정하지 말 것:
     * <ul>
     *   <li>{@code PostReportService} — 게시글을 소프트 삭제(행 보존)하므로 신고
     *       레코드도 함께 보존되고 상태만 {@code POST_DELETED}로 갱신된다.</li>
     *   <li>{@code CommentReportService}/{@code ArtistPhotoReportService} — 댓글/사진
     *       엔티티에 소프트 삭제가 없어 하드 삭제하며, FK 제약상 신고 레코드도
     *       함께 하드 삭제된다(레코드 자체가 사라짐).</li>
     * </ul>
     */
    void deleteContentAndResolve(Long reportId);
    default int bulkDeleteContent(List<Long> ids) {
        int success = 0;
        for (Long id : ids) {
            try {
                deleteContentAndResolve(id);
                success++;
            } catch (Exception e) {
                LoggerFactory.getLogger(ReportCommandService.class).warn("bulk delete failed id={}", id, e);
            }
        }
        return success;
    }
}
