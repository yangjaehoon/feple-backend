package com.feple.feple_backend.timetable.entity;

import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.stage.entity.Stage;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "timetable_entry")
@Getter
@Setter
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

    @Column(nullable = false)
    private String artistName;

    @Column(nullable = false)
    private LocalDate festivalDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    public String getStageName() {
        return stage != null ? stage.getName() : null;
    }
}
