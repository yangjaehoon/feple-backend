package com.feple.feple_backend.badword;

import com.feple.feple_backend.badword.repository.BadWordRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class BadWordFilter {

    private final BadWordRepository badWordRepository;
    private volatile Set<String> badWords = Set.of();

    @PostConstruct
    public void reload() {
        badWords = Set.copyOf(badWordRepository.findAllWords());
    }

    public void validate(String... texts) {
        Set<String> snapshot = badWords;
        if (snapshot.isEmpty()) return;
        for (String text : texts) {
            if (text == null) continue;
            String lower = text.toLowerCase();
            for (String word : snapshot) {
                if (lower.contains(word)) {
                    throw new IllegalArgumentException("금칙어가 포함되어 있습니다.");
                }
            }
        }
    }

    public void validateField(String field, String text) {
        if (text == null) return;
        Set<String> snapshot = badWords;
        if (snapshot.isEmpty()) return;
        String lower = text.toLowerCase();
        for (String word : snapshot) {
            if (lower.contains(word)) {
                throw new IllegalArgumentException(field + ":금칙어가 포함되어 있습니다.");
            }
        }
    }
}
