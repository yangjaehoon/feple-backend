package com.feple.feple_backend.post.entity;

public interface ResolvableReport {
    boolean isPending();
    void resolve(ReportStatus newStatus);
}
