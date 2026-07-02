package com.feple.feple_backend.admin.ocr;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UnmatchedArtistSuggestionRepository extends JpaRepository<UnmatchedArtistSuggestion, Long> {

    @Query("SELECT s FROM UnmatchedArtistSuggestion s ORDER BY s.mentionCount DESC, s.updatedAt DESC")
    List<UnmatchedArtistSuggestion> findAllOrderByMentionCountDesc();

    Optional<UnmatchedArtistSuggestion> findByNameIgnoreCase(String name);
}
