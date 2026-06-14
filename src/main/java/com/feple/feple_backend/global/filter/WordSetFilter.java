package com.feple.feple_backend.global.filter;

import jakarta.annotation.PostConstruct;

import java.util.List;
import java.util.Set;

public abstract class WordSetFilter {

    private volatile Set<String> words = Set.of();

    @PostConstruct
    public void reload() {
        words = Set.copyOf(loadWords());
    }

    protected abstract List<String> loadWords();

    protected boolean containsRestrictedWord(String text) {
        Set<String> snapshot = words;
        if (snapshot.isEmpty()) return false;
        String normalized = text.toLowerCase().replaceAll("\\s+", "");
        return snapshot.stream().anyMatch(normalized::contains);
    }
}
