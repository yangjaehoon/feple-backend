package com.feple.feple_backend.badword.entity;

import com.feple.feple_backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bad_words")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BadWord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String word;

    public BadWord(String word) {
        this.word = word;
    }
}
