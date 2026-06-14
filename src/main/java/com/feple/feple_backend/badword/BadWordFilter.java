package com.feple.feple_backend.badword;

import com.feple.feple_backend.badword.event.BadWordChangedEvent;
import com.feple.feple_backend.badword.repository.BadWordRepository;
import com.feple.feple_backend.global.exception.BadWordException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

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

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChange(BadWordChangedEvent event) {
        reload();
    }

    public void validate(String... texts) {
        Set<String> snapshot = badWords;
        if (snapshot.isEmpty()) return;
        for (String text : texts) {
            if (text == null) continue;
            String normalized = text.toLowerCase().replaceAll("\\s+", "");
            for (String word : snapshot) {
                if (normalized.contains(word)) {
                    throw new IllegalArgumentException("금칙어가 포함되어 있습니다.");
                }
            }
        }
    }

    public void validateField(String field, String text) {
        if (text == null) return;
        Set<String> snapshot = badWords;
        if (snapshot.isEmpty()) return;
        String normalized = text.toLowerCase().replaceAll("\\s+", "");
        for (String word : snapshot) {
            if (normalized.contains(word)) {
                throw new BadWordException(field);
            }
        }
    }
}
