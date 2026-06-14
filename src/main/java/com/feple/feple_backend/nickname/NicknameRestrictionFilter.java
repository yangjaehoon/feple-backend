package com.feple.feple_backend.nickname;

import com.feple.feple_backend.nickname.event.NicknameRestrictionChangedEvent;
import com.feple.feple_backend.nickname.repository.NicknameRestrictionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class NicknameRestrictionFilter {

    private final NicknameRestrictionRepository repository;

    private volatile Set<String> restricted = Set.of();

    @PostConstruct
    public void reload() {
        restricted = Set.copyOf(repository.findAllWords());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChange(NicknameRestrictionChangedEvent event) {
        reload();
    }

    public void validate(String nickname) {
        if (nickname == null) return;
        Set<String> snapshot = restricted;
        if (snapshot.isEmpty()) return;
        String normalized = nickname.toLowerCase().replaceAll("\\s+", "");
        for (String word : snapshot) {
            if (normalized.contains(word)) {
                throw new IllegalArgumentException("닉네임으로 사용할 수 없는 단어가 포함되어 있습니다.");
            }
        }
    }
}
