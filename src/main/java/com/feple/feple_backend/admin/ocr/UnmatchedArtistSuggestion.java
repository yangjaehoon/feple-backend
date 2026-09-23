package com.feple.feple_backend.admin.ocr;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 행 생성·mentionCount 갱신은 {@link UnmatchedArtistSuggestionRepository#upsertMentionCount}의
 * 원자적 업서트가 전담한다(조회·삭제는 일반 리포지토리 메서드 사용). 자바에서 새 인스턴스를 만들어
 * save()하면 동시 언급 시 유니크 제약과 경합해 트랜잭션이 통째로 롤백되므로, 생성용 정적 팩터리를
 * 두지 않는다.
 */
@Entity
@Table(name = "unmatched_artist_suggestion")
@Getter
@NoArgsConstructor
public class UnmatchedArtistSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String name;

    @Column(nullable = false)
    private int mentionCount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
