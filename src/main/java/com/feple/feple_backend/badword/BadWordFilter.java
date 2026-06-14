package com.feple.feple_backend.badword;

import com.feple.feple_backend.badword.event.BadWordChangedEvent;
import com.feple.feple_backend.badword.repository.BadWordRepository;
import com.feple.feple_backend.global.exception.BadWordException;
import com.feple.feple_backend.global.filter.WordSetFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BadWordFilter extends WordSetFilter {

    private final BadWordRepository badWordRepository;

    @Override
    protected List<String> loadWords() {
        return badWordRepository.findAllWords();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChange(BadWordChangedEvent event) {
        reload();
    }

    public void validate(String... texts) {
        for (String text : texts) {
            if (text != null && containsRestrictedWord(text)) {
                throw new IllegalArgumentException("금칙어가 포함되어 있습니다.");
            }
        }
    }

    public void validateField(String field, String text) {
        if (text == null) return;
        if (containsRestrictedWord(text)) {
            throw new BadWordException(field);
        }
    }
}
