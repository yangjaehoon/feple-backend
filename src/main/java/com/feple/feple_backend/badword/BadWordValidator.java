package com.feple.feple_backend.badword;

import com.feple.feple_backend.badword.event.BadWordChangedEvent;
import com.feple.feple_backend.badword.repository.BadWordRepository;
import com.feple.feple_backend.global.exception.BadWordException;
import com.feple.feple_backend.global.filter.BadWordSet;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
@RequiredArgsConstructor
public class BadWordValidator {

    private final BadWordRepository badWordRepository;
    private final BadWordSet wordSet = new BadWordSet();

    @PostConstruct
    public void reloadWords() {
        wordSet.load(badWordRepository.findAllWords());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChange(BadWordChangedEvent event) {
        reloadWords();
    }

    public void validate(String... texts) {
        for (String text : texts) {
            if (text != null && wordSet.contains(text)) {
                throw new IllegalArgumentException("금칙어가 포함되어 있습니다.");
            }
        }
    }

    public void validateField(String field, String text) {
        if (text == null) return;
        if (wordSet.contains(text)) {
            throw new BadWordException(field);
        }
    }
}
