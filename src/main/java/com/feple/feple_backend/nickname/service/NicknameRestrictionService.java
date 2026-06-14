package com.feple.feple_backend.nickname.service;

import com.feple.feple_backend.nickname.entity.NicknameRestriction;
import com.feple.feple_backend.nickname.event.NicknameRestrictionChangedEvent;
import com.feple.feple_backend.nickname.repository.NicknameRestrictionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NicknameRestrictionService {

    private final NicknameRestrictionRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public List<NicknameRestriction> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional
    public void add(String word) {
        String trimmed = word.trim().toLowerCase();
        if (trimmed.isEmpty()) throw new IllegalArgumentException("단어를 입력해 주세요.");
        if (trimmed.length() > 50) throw new IllegalArgumentException("50자 이하여야 합니다.");
        if (repository.existsByWord(trimmed)) throw new IllegalArgumentException("이미 등록된 단어입니다: " + trimmed);
        repository.save(new NicknameRestriction(trimmed));
        eventPublisher.publishEvent(new NicknameRestrictionChangedEvent());
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
        eventPublisher.publishEvent(new NicknameRestrictionChangedEvent());
    }
}
