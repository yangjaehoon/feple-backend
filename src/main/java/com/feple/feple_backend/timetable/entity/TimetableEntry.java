package com.feple.feple_backend.timetable.entity;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.stage.entity.Stage;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "timetable_entry", indexes = {
    @Index(name = "idx_timetable_entry_festival_id", columnList = "festival_id")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimetableEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "festival_id", nullable = false)
    private Festival festival;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id")
    private Stage stage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id")
    private Artist artist;

    @Getter(AccessLevel.NONE)
    @Column(nullable = false)
    private String artistName;

    // 공지/운영 슬롯(아티스트 없는 타임테이블 항목) 판별용 sentinel
    public static final String ANNOUNCEMENT_SENTINEL = "📢";

    // stage(FK)가 연결된 항목은 stage.name을 단일 출처로 쓰고 이 필드는 null이다.
    // stage FK가 없는(OCR 미매칭·공지 슬롯) 항목만 무대명을 이 문자열로 들고 있으며,
    // 스테이지가 삭제될 때 StageService가 삭제 직전 이름을 여기에 스냅샷한다.
    @Column(name = "stage_name")
    private String stageName;

    @Column(nullable = false)
    private LocalDate festivalDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(length = 20)
    private String color;

    @Builder.Default
    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimetableEntryMember> members = new ArrayList<>();

    public void replaceMembers(List<TimetableEntryMember> newMembers) {
        members.clear();
        members.addAll(newMembers);
    }

    public String getArtistName() {
        if (artist != null) return artist.getName();
        return artistName != null ? artistName : "";
    }

    public String getArtistNameEn() {
        return artist != null ? artist.getNameEn() : "";
    }

    public String getStageName() {
        // stage FK가 연결돼 있으면 항상 최신 이름을 반환한다 (getArtistName()과 동일 패턴).
        // FK가 없는 항목은 저장된 폴백 문자열을 그대로 쓴다 — 기존 계약대로 null도 그대로 반환한다.
        if (stage != null) return stage.getName();
        return stageName;
    }

    public Long getFestivalId() {
        return festival != null ? festival.getId() : null;
    }

    public int getStageDisplayOrder() {
        return stage != null ? stage.getDisplayOrder() : Integer.MAX_VALUE;
    }

    public void updateStage(Stage stage) {
        this.stage = stage;
        this.stageName = null;
    }

    public void updateDate(java.time.LocalDate newDate) {
        this.festivalDate = newDate;
    }

    // artist(FK)가 연결되지 않은 항목은 이름을 저장된 문자열로만 들고 있어, 아티스트가
    // 개명해도 자동 반영되지 않는다 — TimetableSyncService가 개명 시 이 문자열을
    // 직접 갱신하기 위한 메서드.
    public void renameArtist(String newName) {
        this.artistName = newName;
    }

    public void update(TimetableEntryFields fields) {
        this.artistName   = fields.artistName();
        this.stage        = fields.stage();
        this.stageName    = fields.stage() != null ? null : fields.stageName();
        this.festivalDate = fields.festivalDate();
        this.startTime    = fields.startTime();
        this.endTime      = fields.endTime();
        this.color        = (fields.color() != null && !fields.color().isBlank()) ? fields.color().trim() : null;
    }
}
