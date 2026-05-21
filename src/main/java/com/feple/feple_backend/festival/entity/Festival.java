package com.feple.feple_backend.festival.entity;

import com.feple.feple_backend.artistfestival.entity.ArtistFestival;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Builder
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Festival {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String titleEn;

    @Column(length = 1000)
    private String description;
    private String location;
    private LocalDate startDate;
    private LocalDate endDate;
    private String posterKey;

    @Builder.Default
    private int likeCount = 0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EventType eventType = EventType.FESTIVAL;

    @ElementCollection(targetClass = Genre.class, fetch = FetchType.LAZY)
    @CollectionTable(name = "festival_genres", joinColumns = @JoinColumn(name = "festival_id"))
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private List<Genre> genres = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Region region;

    private Double latitude;
    private Double longitude;

    @Builder.Default
    @OneToMany(mappedBy = "festival", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ArtistFestival> artistFestivals = new ArrayList<>();

    public void update(String title, String titleEn, String description, String location,
                       LocalDate startDate, LocalDate endDate,
                       List<Genre> genres, Region region, Double latitude, Double longitude) {
        this.title = title;
        this.titleEn = titleEn;
        this.description = description;
        this.location = location;
        this.startDate = startDate;
        this.endDate = endDate;
        if (genres != null) this.genres = genres;
        if (region != null) this.region = region;
        if (latitude != null) this.latitude = latitude;
        if (longitude != null) this.longitude = longitude;
    }

    public void updatePoster(String newKey) {
        if (newKey != null) this.posterKey = newKey;
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        this.likeCount = Math.max(0, this.likeCount - 1);
    }

}
