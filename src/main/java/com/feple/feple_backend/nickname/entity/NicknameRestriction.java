package com.feple.feple_backend.nickname.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "nickname_restrictions")
@Getter
@NoArgsConstructor
public class NicknameRestriction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String word;

    private LocalDateTime createdAt;

    public NicknameRestriction(String word) {
        this.word = word;
        this.createdAt = LocalDateTime.now();
    }
}
