package com.feple.feple_backend.domain.festival;

import com.feple.feple_backend.domain.artist.Artist;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Festival {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 1000)
    private String description;
    private String location;
    private LocalDate startDate;
    private LocalDate endDate;
    private String posterUrl;

    @OneToMany(mappedBy = "festival", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Artist> artists = new ArrayList<>();

    @Builder
    public Festival(String title, String description, String location,
                    LocalDate startDate, LocalDate endDate, String posterUrl,
                    List<Artist> artists) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.startDate = startDate;
        this.endDate = endDate;
        this.posterUrl = posterUrl;
        this.artists = (artists != null) ? artists : new ArrayList<>();
    }
}
