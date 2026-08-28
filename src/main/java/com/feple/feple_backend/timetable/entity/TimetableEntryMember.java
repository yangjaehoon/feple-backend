package com.feple.feple_backend.timetable.entity;

import com.feple.feple_backend.artist.entity.Artist;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "timetable_entry_member", indexes = {
    @Index(name = "idx_timetable_entry_member_entry_id", columnList = "entry_id")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimetableEntryMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id", nullable = false)
    private TimetableEntry entry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id")
    private Artist artist;

    @Getter(AccessLevel.NONE)
    @Column(nullable = false)
    private String artistName;

    public Long getArtistId() {
        return artist != null ? artist.getId() : null;
    }

    // artist에 연결돼 있으면 저장된 문자열 대신 항상 최신 이름을 반환한다 — TimetableEntry.getArtistName()과
    // 동일한 패턴. artist_id로 매칭되지 않은(OCR 미매칭) 멤버는 저장된 원문 텍스트를 그대로 보여준다.
    public String getArtistName() {
        if (artist != null) return artist.getName();
        return artistName != null ? artistName : "";
    }

    public String getArtistNameEn() {
        return artist != null ? artist.getNameEn() : "";
    }
}
