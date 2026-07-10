package com.feple.feple_backend.admin.ocr;

import java.util.List;

/** truncated=true면 Gemini 응답이 maxOutputTokens 한도에 걸려 일부 항목이 누락됐을 수 있음을 의미한다. */
public record OcrParseResult<T>(List<T> entries, boolean truncated) {
}
