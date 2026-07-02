package com.feple.feple_backend.festival.entity;

import com.feple.feple_backend.artistfestival.entity.ArtistFestival;
import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Builder
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(indexes = {
    @Index(name = "idx_festival_like_count", columnList = "like_count DESC"),
    @Index(name = "idx_festival_start_date", columnList = "start_date"),
    @Index(name = "idx_festival_end_date", columnList = "end_date"),
    @Index(name = "idx_festival_region", columnList = "region"),
    @Index(name = "idx_festival_start_like", columnList = "start_date, like_count DESC")
})
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

    @Builder.Default
    private int attendingCount = 0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EventType eventType = EventType.FESTIVAL;

    @BatchSize(size = 50)
    @ElementCollection(targetClass = Genre.class, fetch = FetchType.LAZY)
    @CollectionTable(name = "festival_genres", joinColumns = @JoinColumn(name = "festival_id"))
    @Column(name = "genres", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private List<Genre> genres = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Region region;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Builder.Default
    private AgeRestriction ageRestriction = AgeRestriction.ALL_AGES;

    private Double latitude;
    private Double longitude;

    @Builder.Default
    @OneToMany(mappedBy = "festival")
    private List<ArtistFestival> artistFestivals = new ArrayList<>();

    public void update(String title, String titleEn, String description, String location,
                       LocalDate startDate, LocalDate endDate,
                       List<Genre> genres, Region region, AgeRestriction ageRestriction,
                       Double latitude, Double longitude) {
        this.title = title;
        this.titleEn = titleEn;
        this.description = description;
        this.location = location;
        this.startDate = startDate;
        this.endDate = endDate;
        if (genres != null) this.genres = genres;
        if (region != null) this.region = region;
        if (ageRestriction != null) this.ageRestriction = ageRestriction;
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

    public void incrementAttendingCount() {
        this.attendingCount++;
    }

    public void decrementAttendingCount() {
        this.attendingCount = Math.max(0, this.attendingCount - 1);
    }

}
