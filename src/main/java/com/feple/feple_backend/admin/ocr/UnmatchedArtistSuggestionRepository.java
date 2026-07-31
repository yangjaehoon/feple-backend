package com.feple.feple_backend.admin.ocr;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UnmatchedArtistSuggestionRepository extends JpaRepository<UnmatchedArtistSuggestion, Long> {

    @Query("SELECT s FROM UnmatchedArtistSuggestion s ORDER BY s.mentionCount DESC, s.updatedAt DESC")
    List<UnmatchedArtistSuggestion> findAllOrderByMentionCountDesc();

    Optional<UnmatchedArtistSuggestion> findByNameIgnoreCase(String name);

    // 동시에 여러 OCR 배치가 같은 이름을 언급해도 lost-update 없이 안전하게 증가시키는 원자적 UPDATE.
    // 반환값이 0이면 아직 존재하지 않는 이름 — 호출부에서 새로 저장한다.
    @Modifying
    @Query("UPDATE UnmatchedArtistSuggestion s SET s.mentionCount = s.mentionCount + 1, s.updatedAt = CURRENT_TIMESTAMP "
            + "WHERE LOWER(s.name) = LOWER(:name)")
    int incrementMentionCountByNameIgnoreCase(@Param("name") String name);
}
