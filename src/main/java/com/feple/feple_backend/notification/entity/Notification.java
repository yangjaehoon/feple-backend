package com.feple.feple_backend.notification.entity;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.post.entity.Post;
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
import org.hibernate.annotations.CreationTimestamp;

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

    @Column(nullable = false)
    private boolean isRead = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public static Notification of(User user, NotificationContent content) {
        return base(user, content);
    }

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

    // festival/post/artist는 of() 팩토리에 의해 상호 배타적으로 하나만 채워진다 — referenceId/imageKey를
    // 항상 같은 우선순위(festival → post → artist)로 함께 판별해 두 값이 서로 다른 연관을 가리키지 않게 한다.
    private record Reference(Long id, String imageKey) {}

    private Reference resolveReference() {
        if (festival != null) return new Reference(festival.getId(), festival.getPosterKey());
        if (post != null) return new Reference(post.getId(), post.getFestivalPosterKey());
        if (artist != null) return new Reference(artist.getId(), artist.getProfileImageKey());
        return new Reference(null, null);
    }

    public Long getReferenceId() {
        return resolveReference().id();
    }

    public String getImageKey() {
        return resolveReference().imageKey();
    }

    public void markRead() {
        this.isRead = true;
    }
}
