package com.feple.feple_backend.admin.ocr;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UnmatchedArtistSuggestionService {

    private final UnmatchedArtistSuggestionRepository repository;

    @Transactional
    public void saveAll(List<String> names) {
        for (String name : names) {
            if (name == null || name.isBlank()) continue;
            String trimmed = name.trim();
            repository.findByNameIgnoreCase(trimmed).ifPresentOrElse(
                    UnmatchedArtistSuggestion::incrementMentionCount,
                    () -> repository.save(UnmatchedArtistSuggestion.of(trimmed))
            );
        }
    }

    @Transactional(readOnly = true)
    public List<UnmatchedArtistSuggestionDto> getAll() {
        return repository.findAllOrderByMentionCountDesc()
                .stream().map(UnmatchedArtistSuggestionDto::from).toList();
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
