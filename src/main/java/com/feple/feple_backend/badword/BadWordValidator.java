package com.feple.feple_backend.badword;

import com.feple.feple_backend.badword.entity.BadWord;
import com.feple.feple_backend.badword.event.BadWordChangedEvent;
import com.feple.feple_backend.badword.repository.BadWordRepository;
import com.feple.feple_backend.global.BaseWordListValidator;
import com.feple.feple_backend.global.exception.BadWordException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BadWordValidator extends BaseWordListValidator<BadWord> {

    public BadWordValidator(BadWordRepository repository) {
        super(repository);
    }

    public void reloadWords() {
        reload();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChange(BadWordChangedEvent event) {
        reload();
    }

    public void validate(String... texts) {
        for (String text : texts) {
            if (text != null && contains(text)) {
                throw new IllegalArgumentException("금칙어가 포함되어 있습니다.");
            }
        }
    }

    public void validateField(String field, String text) {
        if (text == null) return;
        if (contains(text)) {
            throw new BadWordException(field);
        }
    }
}
