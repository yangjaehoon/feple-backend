package com.feple.feple_backend.booth.entity;

import com.feple.feple_backend.festival.entity.Festival;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
