package com.feple.feple_backend.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    @Column(nullable = false) @Builder.Default private boolean quietHoursEnabled = false;

    public static NotificationPreference defaultFor(Long userId) {
        return NotificationPreference.builder().userId(userId).build();
    }

    public void update(NotificationPreferenceFields fields) {
        this.certEnabled = fields.certEnabled();
        this.commentEnabled = fields.commentEnabled();
        this.festivalEnabled = fields.festivalEnabled();
        this.songRequestEnabled = fields.songRequestEnabled();
        this.quietHoursEnabled = fields.quietHoursEnabled();
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
