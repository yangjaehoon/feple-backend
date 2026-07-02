package com.feple.feple_backend.badword.service;

import com.feple.feple_backend.badword.entity.BadWord;
import com.feple.feple_backend.badword.event.BadWordChangedEvent;
import com.feple.feple_backend.badword.repository.BadWordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BadWordService {

    private final BadWordRepository badWordRepository;
    private final ApplicationEventPublisher eventPublisher;

    public List<BadWord> findAll() {
        return badWordRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional
    public void add(String word) {
        String trimmed = word.trim().toLowerCase();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("금칙어를 입력해 주세요.");
        }
        if (trimmed.length() > 50)
            throw new IllegalArgumentException("금칙어는 50자 이하여야 합니다.");
        if (badWordRepository.existsByWord(trimmed)) {
            throw new IllegalArgumentException("이미 등록된 금칙어입니다: " + trimmed);
        }
        badWordRepository.save(new BadWord(trimmed));
        eventPublisher.publishEvent(new BadWordChangedEvent());
    }

    @Transactional
    public void delete(Long id) {
        badWordRepository.deleteById(id);
        eventPublisher.publishEvent(new BadWordChangedEvent());
    }
}
