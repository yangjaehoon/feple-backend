package com.feple.feple_backend.global;

import com.feple.feple_backend.global.exception.InvalidRequestException;

import com.feple.feple_backend.global.repository.BaseWordListRepository;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
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
            throw new InvalidRequestException(label() + "를 입력해 주세요.");
        }
        if (trimmed.length() > MAX_WORD_LENGTH) {
            throw new InvalidRequestException(label() + "는 " + MAX_WORD_LENGTH + "자 이하여야 합니다.");
        }
        if (repository.existsByWord(trimmed)) {
            throw new InvalidRequestException("이미 등록된 " + label() + "입니다: " + trimmed);
        }
        // existsByWord 체크 후 save() 사이의 TOCTOU 레이스(동시 등록)는 유니크 제약이 최종 방어선이다 —
        // 위 사전 검증과 동일한 메시지로 변환해준다(AdminActionUtils.tryAction은 InvalidRequestException의
        // 메시지만 그대로 노출하므로 이 타입을 써야 구체적인 안내가 화면에 남는다).
        try {
            repository.save(newEntity(trimmed));
        } catch (DataIntegrityViolationException e) {
            throw new InvalidRequestException("이미 등록된 " + label() + "입니다: " + trimmed);
        }
        eventPublisher.publishEvent(changedEvent());
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
        eventPublisher.publishEvent(changedEvent());
    }
}
