package com.feple.feple_backend.booth.entity;

import com.feple.feple_backend.festival.entity.Festival;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "booth")
@Getter
@Setter
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

    private String imageUrl;
}
