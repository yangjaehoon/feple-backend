package com.feple.feple_backend.nickname;

import com.feple.feple_backend.global.BaseWordListValidator;
import com.feple.feple_backend.nickname.entity.NicknameRestriction;
import com.feple.feple_backend.nickname.event.NicknameRestrictionChangedEvent;
import com.feple.feple_backend.nickname.repository.NicknameRestrictionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NicknameRestrictionValidator extends BaseWordListValidator<NicknameRestriction> {

    public NicknameRestrictionValidator(NicknameRestrictionRepository repository) {
        super(repository);
    }

    public void reloadRestrictions() {
        reload();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChange(NicknameRestrictionChangedEvent event) {
        reload();
    }

    public void validate(String nickname) {
        if (nickname == null) return;
        if (contains(nickname)) {
            throw new IllegalArgumentException("닉네임으로 사용할 수 없는 단어가 포함되어 있습니다.");
        }
    }
}
