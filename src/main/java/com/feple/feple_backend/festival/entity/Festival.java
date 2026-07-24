package com.feple.feple_backend.festival.entity;

import com.feple.feple_backend.artistfestival.entity.ArtistFestival;
import com.feple.feple_backend.global.MusicGenre;
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

    // 관리자 동시 편집 시 lost update 방지
    @Version
    private Long version;

    @Column(nullable = false)
    private String title;

    private String titleEn;

    @Column(length = 1000)
    private String description;
    private String location;
    private LocalDate startDate;
    private LocalDate endDate;
    @Column(length = 500)
    private String posterKey;

    @Builder.Default
    private int likeCount = 0;

    @Builder.Default
    private int attendingCount = 0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EventType eventType = EventType.FESTIVAL;

    @BatchSize(size = 50)
    @ElementCollection(targetClass = MusicGenre.class, fetch = FetchType.LAZY)
    @CollectionTable(name = "festival_genres", joinColumns = @JoinColumn(name = "festival_id"))
    @Column(name = "genres", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private List<MusicGenre> genres = new ArrayList<>();

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

    public void update(FestivalUpdateFields fields) {
        this.title = fields.title();
        this.titleEn = fields.titleEn();
        this.description = fields.description();
        this.location = fields.location();
        this.startDate = fields.startDate();
        this.endDate = fields.endDate();
        if (fields.genres() != null) this.genres = fields.genres();
        if (fields.region() != null) this.region = fields.region();
        if (fields.ageRestriction() != null) this.ageRestriction = fields.ageRestriction();
        if (fields.latitude() != null) this.latitude = fields.latitude();
        if (fields.longitude() != null) this.longitude = fields.longitude();
    }

    public void updatePoster(String newKey) {
        if (newKey != null) this.posterKey = newKey;
    }

}
