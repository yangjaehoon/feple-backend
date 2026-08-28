package com.feple.feple_backend.diary.entity;

import com.feple.feple_backend.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "festival_diary_photo",
    indexes = {
        @Index(name = "idx_festival_diary_photo_diary", columnList = "diary_id")
    }
)
public class FestivalDiaryPhoto extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id", nullable = false)
    private FestivalDiary diary;

    @Column(name = "photo_key", nullable = false, length = 500)
    private String photoKey;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public Long getDiaryId() { return diary.getId(); }

    public static FestivalDiaryPhoto create(FestivalDiary diary, String photoKey, int sortOrder) {
        FestivalDiaryPhoto photo = new FestivalDiaryPhoto();
        photo.diary = diary;
        photo.photoKey = photoKey;
        photo.sortOrder = sortOrder;
        return photo;
    }
}
