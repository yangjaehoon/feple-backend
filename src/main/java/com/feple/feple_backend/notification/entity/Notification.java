package com.feple.feple_backend.notification.entity;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications",
        indexes = @Index(name = "idx_notification_user_id_created_at", columnList = "user_id, created_at DESC"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 255)
    private String body;

    @Column(length = 100)
    private String titleEn;

    @Column(length = 255)
    private String bodyEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "festival_id")
    private Festival festival;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id")
    private Artist artist;

    public Long getReferenceId() {
        if (festival != null) return festival.getId();
        if (post != null) return post.getId();
        if (artist != null) return artist.getId();
        return null;
    }

    @Column(nullable = false)
    private boolean isRead = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public static Notification of(User user, NotificationContent content, Festival festival) {
        Notification notification = base(user, content);
        notification.festival = festival;
        return notification;
    }

    public static Notification of(User user, NotificationContent content, Post post) {
        Notification notification = base(user, content);
        notification.post = post;
        return notification;
    }

    public static Notification of(User user, NotificationContent content, Artist artist) {
        Notification notification = base(user, content);
        notification.artist = artist;
        return notification;
    }

    private static Notification base(User user, NotificationContent content) {
        Notification notification = new Notification();
        notification.user = user;
        notification.type = content.type();
        notification.title = content.title();
        notification.body = content.body();
        notification.titleEn = content.titleEn();
        notification.bodyEn = content.bodyEn();
        return notification;
    }

    public Long getUserId() { return user.getId(); }

    public String getImageKey() {
        if (festival != null) return festival.getPosterKey();
        if (artist != null) return artist.getProfileImageKey();
        if (post != null && post.getFestivalPosterKey() != null) return post.getFestivalPosterKey();
        return null;
    }

    public void markRead() {
        this.isRead = true;
    }
}
