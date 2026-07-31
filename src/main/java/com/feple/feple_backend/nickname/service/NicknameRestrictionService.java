package com.feple.feple_backend.nickname.service;

import com.feple.feple_backend.global.BaseWordListService;
import com.feple.feple_backend.nickname.entity.NicknameRestriction;
import com.feple.feple_backend.nickname.event.NicknameRestrictionChangedEvent;
import com.feple.feple_backend.nickname.repository.NicknameRestrictionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class NicknameRestrictionService extends BaseWordListService<NicknameRestriction> {

    public NicknameRestrictionService(NicknameRestrictionRepository repository, ApplicationEventPublisher eventPublisher) {
        super(repository, eventPublisher);
    }

    @Override
    protected NicknameRestriction newEntity(String word) {
        return new NicknameRestriction(word);
    }

    @Override
    protected Object changedEvent() {
        return new NicknameRestrictionChangedEvent();
    }

    @Override
    protected String label() {
        return "단어";
    }
}
