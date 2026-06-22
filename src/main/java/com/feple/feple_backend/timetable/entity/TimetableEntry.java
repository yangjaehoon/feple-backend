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
        if (artistName != null && !artistName.isBlank()) return artistName;
        return artist != null ? artist.getName() : "";
    }

    public String getStageName() {
        if (stageName != null) return stageName;
        return stage != null ? stage.getName() : null;
    }

    public Long getFestivalId() {
        return festival != null ? festival.getId() : null;
    }

    public void updateStage(Stage stage) {
        this.stage = stage;
        this.stageName = stage.getName();
    }

    public void updateDate(java.time.LocalDate newDate) {
        this.festivalDate = newDate;
    }

    public void update(String artistName, String stageName, Stage stage,
                       java.time.LocalDate festivalDate,
                       java.time.LocalTime startTime, java.time.LocalTime endTime,
                       String color) {
        this.artistName   = artistName;
        this.stageName    = stageName;
        this.stage        = stage;
        this.festivalDate = festivalDate;
        this.startTime    = startTime;
        this.endTime      = endTime;
        this.color        = (color != null && !color.isBlank()) ? color.trim() : null;
    }
}
