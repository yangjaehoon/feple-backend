package com.feple.feple_backend.timetable.entity;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.stage.entity.Stage;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "stage_name", nullable = false)
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
        if (stageName != null) return stageName;
        return stage != null ? stage.getName() : null;
    }

    public boolean isAnnouncement() {
        return ANNOUNCEMENT_SENTINEL.equals(stageName);
    }

    public Long getFestivalId() {
        return festival != null ? festival.getId() : null;
    }

    public int getStageDisplayOrder() {
        return stage != null ? stage.getDisplayOrder() : Integer.MAX_VALUE;
    }

    public void updateStage(Stage stage) {
        this.stage = stage;
        this.stageName = stage.getName();
    }

    public void updateDate(java.time.LocalDate newDate) {
        this.festivalDate = newDate;
    }

    public void update(TimetableEntryFields fields) {
        this.artistName   = fields.artistName();
        this.stageName    = fields.stageName();
        this.stage        = fields.stage();
        this.festivalDate = fields.festivalDate();
        this.startTime    = fields.startTime();
        this.endTime      = fields.endTime();
        this.color        = (fields.color() != null && !fields.color().isBlank()) ? fields.color().trim() : null;
    }
}
