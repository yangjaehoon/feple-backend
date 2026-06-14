package com.feple.feple_backend.nickname.entity;

import com.feple.feple_backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "nickname_restrictions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NicknameRestriction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String word;

    public NicknameRestriction(String word) {
        this.word = word;
    }
}
