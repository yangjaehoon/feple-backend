package com.feple.feple_backend.admin.service;

import java.util.List;

public interface ReportCommandService {
    void dismissReport(Long reportId);
    void bulkDismiss(List<Long> ids);
    void deleteContentAndResolve(Long reportId);
    default void bulkDeleteContent(List<Long> ids) {
        for (Long id : ids) {
            try { deleteContentAndResolve(id); } catch (Exception ignored) {}
        }
    }
}
