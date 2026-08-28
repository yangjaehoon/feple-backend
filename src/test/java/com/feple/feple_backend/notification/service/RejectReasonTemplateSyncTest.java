package com.feple.feple_backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * 관리자 화면의 반려 사유(칩/셀렉트 값)와 NotificationRejectReasons의 한→영 매핑 키가
 * 글자 단위로 일치하는지 CI에서 강제한다. 불일치 시 영문 사용자에게 한국어 원문이 노출된다.
 */
class RejectReasonTemplateSyncTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates/admin");

    @Test
    void 인증_반려사유_템플릿과_매핑_일치() {
        assertReasonsMatch(NotificationRejectReasons.CERT,
                extract("certification/detail.html", "setCertReason\\(this,\\s*'([^']+)'\\)"));
    }

    @Test
    void 노래요청_반려사유_템플릿과_매핑_일치() {
        assertReasonsMatch(NotificationRejectReasons.SONG,
                extract("song-request/list.html", "<option value=\"([^\"]+)\""));
    }

    @Test
    void 아티스트_신청사유_템플릿과_매핑_일치() {
        Set<String> fromTemplates = new LinkedHashSet<>();
        fromTemplates.addAll(extract("artist/suggestions.html", "data-reason=\"([^\"]+)\""));
        fromTemplates.addAll(extract("artist/list.html", "data-reason=\"([^\"]+)\""));
        assertReasonsMatch(NotificationRejectReasons.ARTIST, fromTemplates);
    }

    @Test
    void 페스티벌_신청사유_템플릿과_매핑_일치() {
        assertReasonsMatch(NotificationRejectReasons.FESTIVAL,
                extract("festival/suggestions.html", "data-reason=\"([^\"]+)\""));
    }

    private void assertReasonsMatch(Map<String, String> reasonMap, Set<String> fromTemplates) {
        assertThat(fromTemplates).isNotEmpty();
        assertThat(fromTemplates)
                .as("템플릿 사유 문자열과 NotificationRejectReasons 키가 정확히 일치해야 한다")
                .containsExactlyInAnyOrderElementsOf(reasonMap.keySet());
    }

    private Set<String> extract(String relativePath, String regex) {
        Path file = TEMPLATES.resolve(relativePath);
        assertThat(Files.exists(file)).as("템플릿 존재: %s", file).isTrue();
        String content;
        try {
            content = Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Set<String> found = new LinkedHashSet<>();
        Matcher m = Pattern.compile(regex).matcher(content);
        while (m.find()) {
            String value = m.group(1).trim();
            if (!value.isEmpty()) {
                found.add(value);
            }
        }
        return found;
    }

    @Test
    void 매핑에_빈_영문값이_없다() {
        List<Map<String, String>> all = List.of(NotificationRejectReasons.ARTIST, NotificationRejectReasons.FESTIVAL,
                NotificationRejectReasons.CERT, NotificationRejectReasons.SONG);
        for (Map<String, String> map : all) {
            assertThat(map.values()).allSatisfy(en -> assertThat(en).isNotBlank());
        }
    }
}
