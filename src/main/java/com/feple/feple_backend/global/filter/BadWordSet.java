package com.feple.feple_backend.global.filter;

import java.util.List;
import java.util.Set;

public class BadWordSet {

    private volatile Set<String> words = Set.of();

    public void load(List<String> wordList) {
        this.words = Set.copyOf(wordList);
    }

    public boolean contains(String text) {
        Set<String> snapshot = words;
        if (snapshot.isEmpty()) return false;
        String normalized = text.toLowerCase().replaceAll("\\s+", "");
        return snapshot.stream().anyMatch(normalized::contains);
    }
}
