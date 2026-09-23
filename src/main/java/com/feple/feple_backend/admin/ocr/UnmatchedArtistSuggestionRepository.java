package com.feple.feple_backend.admin.ocr;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UnmatchedArtistSuggestionRepository extends JpaRepository<UnmatchedArtistSuggestion, Long> {

    @Query("SELECT s FROM UnmatchedArtistSuggestion s ORDER BY s.mentionCount DESC, s.updatedAt DESC")
    List<UnmatchedArtistSuggestion> findAllOrderByMentionCountDesc();

    // 동시에 여러 OCR 배치가 같은 이름을 언급해도 lost-update 없이 안전하게 집계하는 원자적 업서트.
    // "UPDATE 후 0건이면 INSERT" 2단계로 나누면 신규 이름을 두 배치가 동시에 처음 언급할 때 유니크
    // 제약(uq_unmatched_name)에서 경합하고, 그 예외를 호출부에서 catch해도 참여 트랜잭션은 이미
    // rollback-only라 커밋이 통째로 실패한다. 한 문장으로 합쳐 예외 경로 자체를 없앤다.
    // name 컬럼은 MySQL 기본 대소문자 무시 콜레이션이라 기존 LOWER() 비교와 동일하게 동작한다.
    @Modifying
    @Query(value = "INSERT INTO unmatched_artist_suggestion (name, mention_count, created_at, updated_at) "
            + "VALUES (:name, 1, NOW(), NOW()) "
            + "ON DUPLICATE KEY UPDATE mention_count = mention_count + 1, updated_at = NOW()",
            nativeQuery = true)
    void upsertMentionCount(@Param("name") String name);
}
