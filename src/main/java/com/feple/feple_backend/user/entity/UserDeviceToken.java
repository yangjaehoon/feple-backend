package com.feple.feple_backend.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_device_tokens",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "token"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserDeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 512)
    private String token;

    @Column(length = 10)
    private String platform; // "android" | "ios"

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public static UserDeviceToken of(User user, String token, String platform) {
        UserDeviceToken t = new UserDeviceToken();
        t.user = user;
        t.token = token;
        t.platform = platform;
        return t;
    }
}
