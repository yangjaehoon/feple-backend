package com.feple.feple_backend.global.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * 금칙어·닉네임 제한어처럼 "단어 하나 + 생성일시"만 갖는 단순 목록형 Repository 공통 계약.
 * #{#entityName}은 Spring Data가 구현체의 실제 엔티티 타입으로 치환한다.
 */
@NoRepositoryBean
public interface BaseWordListRepository<T> extends JpaRepository<T, Long> {
    boolean existsByWord(String word);

    @Query("SELECT w.word FROM #{#entityName} w")
    List<String> findAllWords();
}
