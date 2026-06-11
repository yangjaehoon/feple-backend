package com.feple.feple_backend.admin;

public interface ReportCsvExporter {
    String getReportType();
    String buildCsv();
}
