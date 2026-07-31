package com.feple.feple_backend.notification.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "notification_preferences")
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false) @Builder.Default private boolean certEnabled = true;
    @Column(nullable = false) @Builder.Default private boolean commentEnabled = true;
    @Column(nullable = false) @Builder.Default private boolean festivalEnabled = true;
    @Column(nullable = false) @Builder.Default private boolean songRequestEnabled = true;

    public static NotificationPreference defaultFor(Long userId) {
        return NotificationPreference.builder().userId(userId).build();
    }

    public void update(NotificationPreferenceFields fields) {
        this.certEnabled = fields.certEnabled();
        this.commentEnabled = fields.commentEnabled();
        this.festivalEnabled = fields.festivalEnabled();
        this.songRequestEnabled = fields.songRequestEnabled();
    }

    public boolean isEnabledFor(NotificationType type) {
        return switch (type.getCategory()) {
            case CERTIFICATION -> certEnabled;
            case COMMENT -> commentEnabled;
            case FESTIVAL -> festivalEnabled;
            case SONG_REQUEST -> songRequestEnabled;
            case ALWAYS_ENABLED -> true;
        };
    }
}
