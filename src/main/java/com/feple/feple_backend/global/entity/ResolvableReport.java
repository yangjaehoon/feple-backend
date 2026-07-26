package com.feple.feple_backend.global.entity;

public interface ResolvableReport {
    boolean isPending();
    void resolve(ReportStatus newStatus);
}
