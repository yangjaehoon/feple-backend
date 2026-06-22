package com.feple.feple_backend.timetable.entity;

import com.feple.feple_backend.artist.entity.Artist;
import jakarta.persistence.*;
import lombok.*;

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

    @Column(nullable = false)
    private String artistName;

    public Long getArtistId() {
        return artist != null ? artist.getId() : null;
    }
}
