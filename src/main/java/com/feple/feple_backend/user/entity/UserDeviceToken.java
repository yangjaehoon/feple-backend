package com.feple.feple_backend.user.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "user_device_tokens",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "token"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserDeviceToken {

    // 언어 미지정/빈 값 시 기본값 — 이 클래스가 유일한 출처: 호출부(DeviceTokenService/컨트롤러)는
    // 별도로 기본값을 채우지 않고 원본 값을 그대로 넘긴다.
    private static final String DEFAULT_LANGUAGE = "ko";

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
    private String language = DEFAULT_LANGUAGE;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public static UserDeviceToken of(User user, DeviceTokenRegistration registration, DevicePlatform platform) {
        UserDeviceToken deviceToken = new UserDeviceToken();
        deviceToken.user = user;
        deviceToken.token = registration.token();
        deviceToken.platform = platform;
        String language = registration.language();
        deviceToken.language = (language != null && !language.isBlank()) ? language : DEFAULT_LANGUAGE;
        return deviceToken;
    }

    public void updateLanguage(String language) {
        if (language != null && !language.isBlank()) {
            this.language = language;
        }
    }
}
