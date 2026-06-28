package com.feple.feple_backend.admin.scraper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class FestivalDateParser {

    static final Pattern DATE_PATTERN = Pattern.compile(
        "(\\d{4})[.\\-/년]\\s*(\\d{1,2})[.\\-/월]\\s*(\\d{1,2})"
    );

    private FestivalDateParser() {}

    static String[] parseRange(String raw) {
        Matcher m = DATE_PATTERN.matcher(raw);
        if (!m.find()) return new String[]{"", ""};
        String start = format(m.group(1), m.group(2), m.group(3));
        String end   = m.find() ? format(m.group(1), m.group(2), m.group(3)) : start;
        return new String[]{start, end};
    }

    static String normalize(String raw) {
        if (raw == null || raw.isBlank()) return "";
        if (raw.matches("\\d{4}-\\d{2}-\\d{2}.*")) return raw.substring(0, 10);
        return parseRange(raw)[0];
    }

    static String extractJsonValue(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        return m.find() ? m.group(1) : "";
    }

    private static String format(String year, String month, String day) {
        try {
            return LocalDate.of(
                    Integer.parseInt(year),
                    Integer.parseInt(month.trim()),
                    Integer.parseInt(day.trim())
            ).format(DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return "";
        }
    }
}
