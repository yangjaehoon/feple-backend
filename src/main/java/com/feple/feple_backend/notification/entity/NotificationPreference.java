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

    public void update(boolean certEnabled, boolean commentEnabled,
                       boolean festivalEnabled, boolean songRequestEnabled) {
        this.certEnabled = certEnabled;
        this.commentEnabled = commentEnabled;
        this.festivalEnabled = festivalEnabled;
        this.songRequestEnabled = songRequestEnabled;
    }

    public boolean isEnabledFor(NotificationType type) {
        return switch (type) {
            case CERT_APPROVED, CERT_REJECTED -> certEnabled;
            case NEW_COMMENT, NEW_REPLY, POST_LIKED -> commentEnabled;
            case NEW_FESTIVAL, FESTIVAL_REMINDER -> festivalEnabled;
            case SONG_REQUEST_APPROVED, SONG_REQUEST_REJECTED,
                 ARTIST_SUGGESTION_PROCESSED -> songRequestEnabled;
            case ADMIN_BROADCAST, POST_DELETED_BY_ADMIN -> true;
        };
    }
}
