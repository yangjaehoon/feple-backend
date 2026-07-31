package com.feple.feple_backend.global;

import com.feple.feple_backend.global.filter.BadWordSet;
import com.feple.feple_backend.global.repository.BaseWordListRepository;
import jakarta.annotation.PostConstruct;

/** 금칙어·닉네임 제한어처럼 DB에서 단어 목록을 읽어와 메모리에 캐시해두고 포함 여부를 검사하는 검증기 공통 로직 */
public abstract class BaseWordListValidator<T> {

    private final BaseWordListRepository<T> repository;
    private final BadWordSet wordSet = new BadWordSet();

    protected BaseWordListValidator(BaseWordListRepository<T> repository) {
        this.repository = repository;
    }

    @PostConstruct
    protected void reload() {
        wordSet.load(repository.findAllWords());
    }

    protected boolean contains(String text) {
        return wordSet.contains(text);
    }
}
