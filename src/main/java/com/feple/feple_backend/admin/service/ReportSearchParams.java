package com.feple.feple_backend.admin.service;

public record ReportSearchParams(int page, int size, String statusFilter, String keyword) {}
