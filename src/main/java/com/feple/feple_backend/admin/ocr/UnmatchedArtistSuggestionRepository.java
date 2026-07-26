package com.feple.feple_backend.admin.ocr;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UnmatchedArtistSuggestionRepository extends JpaRepository<UnmatchedArtistSuggestion, Long> {

    @Query("SELECT s FROM UnmatchedArtistSuggestion s ORDER BY s.mentionCount DESC, s.updatedAt DESC")
    List<UnmatchedArtistSuggestion> findAllOrderByMentionCountDesc();

    Optional<UnmatchedArtistSuggestion> findByNameIgnoreCase(String name);
}
