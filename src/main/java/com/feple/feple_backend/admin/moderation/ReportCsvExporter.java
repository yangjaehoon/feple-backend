package com.feple.feple_backend.admin.moderation;

public interface ReportCsvExporter {
    String getReportType();
    String buildCsv();
}
