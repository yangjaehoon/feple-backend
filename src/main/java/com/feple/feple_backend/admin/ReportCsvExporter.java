package com.feple.feple_backend.admin;

interface ReportCsvExporter {
    String getReportType();
    String buildCsv();
}
