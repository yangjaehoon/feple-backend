package com.feple.feple_backend.admin.account;

import com.feple.feple_backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

import static jakarta.persistence.EnumType.STRING;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "admin_accounts")
@Getter
@NoArgsConstructor(access = PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class AdminAccount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 관리자 동시 편집 시 lost update 방지
    @Version
    private Long version;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(length = 50)
    private String displayName;

    @Enumerated(STRING)
    @Column(nullable = false, length = 20)
    private AdminRole role;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "admin_account_permissions",
            joinColumns = @JoinColumn(name = "admin_account_id")
    )
    @Enumerated(STRING)
    @Column(name = "permission", length = 30)
    @Builder.Default
    private Set<AdminPermission> permissions = new HashSet<>();

    @Column(length = 512)
    private String profileImageUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    public void updateProfileImage(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void updateProfile(String displayName, AdminRole role, Set<AdminPermission> permissions) {
        this.displayName = displayName;
        this.role = role;
        this.permissions.clear();
        this.permissions.addAll(permissions);
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void toggle() {
        this.enabled = !this.enabled;
    }
}
