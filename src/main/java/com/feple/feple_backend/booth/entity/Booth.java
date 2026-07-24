package com.feple.feple_backend.booth.entity;

import com.feple.feple_backend.festival.entity.Festival;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "booth", indexes = {
    @Index(name = "idx_booth_festival_id", columnList = "festival_id")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "festival_id", nullable = false)
    private Festival festival;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoothType boothType;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    private String description;

    @Column(length = 500)
    private String imageKey;

    public Long getFestivalId() {
        return festival != null ? festival.getId() : null;
    }

    public String getBoothTypeDisplayName() {
        return boothType != null ? boothType.getDisplayName() : null;
    }
}
