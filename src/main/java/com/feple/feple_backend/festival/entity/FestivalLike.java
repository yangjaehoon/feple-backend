package com.feple.feple_backend.festival.entity;

import com.feple.feple_backend.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "festival_id"})
})
public class FestivalLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "festival_id", nullable = false)
    private Festival festival;

    public static FestivalLike of(User user, Festival festival) {
        FestivalLike fl = new FestivalLike();
        fl.user = user;
        fl.festival = festival;
        return fl;
    }
}
