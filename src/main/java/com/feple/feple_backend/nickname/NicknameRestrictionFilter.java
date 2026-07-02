package com.feple.feple_backend.nickname;

import com.feple.feple_backend.global.filter.WordSet;
import com.feple.feple_backend.nickname.event.NicknameRestrictionChangedEvent;
import com.feple.feple_backend.nickname.repository.NicknameRestrictionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
@RequiredArgsConstructor
public class NicknameRestrictionFilter {

    private final NicknameRestrictionRepository repository;
    private final WordSet wordSet = new WordSet();

    @PostConstruct
    public void reload() {
        wordSet.load(repository.findAllWords());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChange(NicknameRestrictionChangedEvent event) {
        reload();
    }

    public void validate(String nickname) {
        if (nickname == null) return;
        if (wordSet.contains(nickname)) {
            throw new IllegalArgumentException("닉네임으로 사용할 수 없는 단어가 포함되어 있습니다.");
        }
    }
}
