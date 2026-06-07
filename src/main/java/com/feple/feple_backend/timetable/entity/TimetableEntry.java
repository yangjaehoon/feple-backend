package com.feple.feple_backend.timetable.entity;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.stage.entity.Stage;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "timetable_entry")
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

    public String getArtistName() {
        if (artistName != null && !artistName.isBlank()) return artistName;
        return artist != null ? artist.getName() : "";
    }

    public String getStageName() {
        if (stageName != null) return stageName;
        return stage != null ? stage.getName() : null;
    }

    public void updateStage(Stage stage) {
        this.stage = stage;
    }
}
