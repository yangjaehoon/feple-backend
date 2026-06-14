package com.feple.feple_backend.post.entity;

public interface Resolvable {
    boolean isPending();
    void resolve(ReportStatus newStatus);
}
