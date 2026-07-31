package com.feple.feple_backend.badword.service;

import com.feple.feple_backend.badword.entity.BadWord;
import com.feple.feple_backend.badword.event.BadWordChangedEvent;
import com.feple.feple_backend.badword.repository.BadWordRepository;
import com.feple.feple_backend.global.BaseWordListService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class BadWordService extends BaseWordListService<BadWord> {

    public BadWordService(BadWordRepository repository, ApplicationEventPublisher eventPublisher) {
        super(repository, eventPublisher);
    }

    @Override
    protected BadWord newEntity(String word) {
        return new BadWord(word);
    }

    @Override
    protected Object changedEvent() {
        return new BadWordChangedEvent();
    }

    @Override
    protected String label() {
        return "금칙어";
    }
}
