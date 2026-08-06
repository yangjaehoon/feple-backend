package com.feple.feple_backend.admin.csv;

public interface ReportCsvExporter {
    String getReportType();
    String buildCsv();
}
