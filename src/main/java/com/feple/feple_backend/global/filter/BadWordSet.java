package com.feple.feple_backend.global.filter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BadWordSet {

    private volatile Set<String> words = Set.of();

    public void load(List<String> wordList) {
        // 등록된 금칙어도 입력 텍스트와 동일하게 정규화해야, 구분자가 포함된 등록어(예: "나쁜 말")도
        // 정규화된 입력과 일관되게 비교된다
        this.words = wordList.stream().map(this::normalize).collect(Collectors.toUnmodifiableSet());
    }

    public boolean contains(String text) {
        Set<String> snapshot = words;
        if (snapshot.isEmpty()) return false;
        String normalized = normalize(text);
        return snapshot.stream().anyMatch(normalized::contains);
    }

    /**
     * 공백뿐 아니라 구두점·기호·zero-width 문자 등 글자/숫자가 아닌 모든 문자를 제거해 매칭한다.
     * "바.보", "바-보", "b_a_d" 처럼 글자 사이에 구분자를 끼워 필터를 우회하는 방식을 막는다.
     * (\p{L}=유니코드 문자, \p{N}=숫자 — 한글 포함)
     */
    private String normalize(String text) {
        return text.toLowerCase().replaceAll("[^\\p{L}\\p{N}]", "");
    }
}
