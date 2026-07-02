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

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private DevicePlatform platform;

    @Column(length = 10, nullable = false)
    private String language = "ko";

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public static UserDeviceToken of(User user, String token, DevicePlatform platform, String language) {
        UserDeviceToken deviceToken = new UserDeviceToken();
        deviceToken.user = user;
        deviceToken.token = token;
        deviceToken.platform = platform;
        deviceToken.language = (language != null && !language.isBlank()) ? language : "ko";
        return deviceToken;
    }

    public void updateLanguage(String language) {
        if (language != null && !language.isBlank()) {
            this.language = language;
        }
    }
}
