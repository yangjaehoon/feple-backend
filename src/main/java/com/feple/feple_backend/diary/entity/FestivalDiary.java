package com.feple.feple_backend.diary.entity;

import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.global.entity.BaseTimeEntity;
import com.feple.feple_backend.user.entity.User;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "festival_diary",
    indexes = {
        @Index(name = "idx_festival_diary_festival_visibility", columnList = "festival_id, visibility"),
        @Index(name = "idx_festival_diary_user", columnList = "user_id")
    }
)
public class FestivalDiary extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "festival_id", nullable = false)
    private Festival festival;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiaryVisibility visibility;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Long getUserId() { return user.getId(); }
    public String getUserNickname() { return user.getNickname(); }
    public Long getFestivalId() { return festival.getId(); }
    public String getFestivalTitle() { return festival.getTitle(); }
    public String getFestivalTitleEn() { return festival.getTitleEn(); }

    public boolean isPublic() { return visibility == DiaryVisibility.PUBLIC; }

    public static FestivalDiary create(User user, Festival festival, String content, DiaryVisibility visibility) {
        FestivalDiary diary = new FestivalDiary();
        diary.user = user;
        diary.festival = festival;
        diary.content = content;
        diary.visibility = visibility;
        return diary;
    }

    public void update(String content, DiaryVisibility visibility) {
        this.content = content;
        this.visibility = visibility;
    }
}
