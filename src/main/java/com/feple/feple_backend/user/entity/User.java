package com.feple.feple_backend.user.entity;

import com.feple.feple_backend.comment.entity.Comment;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"provider", "oauth_id"})
        }
        )
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nickname;

    @Column(nullable = false)
    private String oauthId;

    @Enumerated(EnumType.STRING)
    private AuthProvider provider;

    private String profileImageUrl;

    @Column(nullable = true)
    private String email;

    @Column(nullable = true)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public void changeRole(UserRole newRole) {
        this.role = newRole;
    }

    public void changeNickname(String newNickname) {
        this.nickname = newNickname;
    }

    public void changeProfileImage(String imageUrl) {
        this.profileImageUrl = imageUrl;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

}
