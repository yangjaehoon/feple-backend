package com.feple.feple_backend.global;

import com.feple.feple_backend.global.repository.BaseWordListRepository;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

/** 금칙어·닉네임 제한어처럼 "단어 등록/삭제 + 변경 이벤트 발행" 형태의 관리자 CRUD 서비스 공통 로직 */
@Transactional(readOnly = true)
public abstract class BaseWordListService<T> {

    private static final int MAX_WORD_LENGTH = 50;

    private final BaseWordListRepository<T> repository;
    private final ApplicationEventPublisher eventPublisher;

    protected BaseWordListService(BaseWordListRepository<T> repository, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    protected abstract T newEntity(String word);
    protected abstract Object changedEvent();
    protected abstract String label();

    public List<T> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional
    public void add(String word) {
        String trimmed = word.trim().toLowerCase();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(label() + "를 입력해 주세요.");
        }
        if (trimmed.length() > MAX_WORD_LENGTH) {
            throw new IllegalArgumentException(label() + "는 " + MAX_WORD_LENGTH + "자 이하여야 합니다.");
        }
        if (repository.existsByWord(trimmed)) {
            throw new IllegalArgumentException("이미 등록된 " + label() + "입니다: " + trimmed);
        }
        repository.save(newEntity(trimmed));
        eventPublisher.publishEvent(changedEvent());
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
        eventPublisher.publishEvent(changedEvent());
    }
}
