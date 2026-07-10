package com.feple.feple_backend.admin.service;

import org.slf4j.LoggerFactory;

import java.util.List;

public interface ReportCommandService {
    void dismissReport(Long reportId);
    void bulkDismiss(List<Long> ids);
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
