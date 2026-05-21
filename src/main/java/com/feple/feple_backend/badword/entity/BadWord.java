package com.feple.feple_backend.badword.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "bad_words")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BadWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String word;

    private LocalDateTime createdAt;

    public BadWord(String word) {
        this.word = word;
        this.createdAt = LocalDateTime.now();
    }
}
