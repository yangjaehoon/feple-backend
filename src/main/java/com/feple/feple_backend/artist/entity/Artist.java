package com.feple.feple_backend.artist.entity;

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
    @Builder.Default
    private int followerCount = 0;

    public void increaseFollowCount() {
        this.followerCount++;
    }

    public void decreaseFollowCount() {
        if (this.followerCount > 0) this.followerCount--;
    }
}
