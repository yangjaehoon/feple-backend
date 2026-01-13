package com.feple.feple_backend.artist.domain;

import com.feple.feple_backend.festival.domain.Festival;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Artist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String genre;
    private String profileImageUrl;

    @Column(nullable = false)
    private int likeCount = 0;
}
