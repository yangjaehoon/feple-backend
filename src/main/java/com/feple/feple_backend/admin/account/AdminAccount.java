package com.feple.feple_backend.admin.account;

import static jakarta.persistence.EnumType.STRING;
import static lombok.AccessLevel.PROTECTED;

import com.feple.feple_backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import java.util.HashMap;
import java.util.Map;
import lombok.*;

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

    // permission → level(READ/WRITE) 매핑. 컬렉션 테이블은 (admin_account_id, permission) 당 한 행,
    // level 컬럼에 수준을 저장한다. SUPER_ADMIN은 이 맵을 쓰지 않고 로그인 시점에 전 권한을 동적 부여한다.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "admin_account_permissions",
            joinColumns = @JoinColumn(name = "admin_account_id")
    )
    @MapKeyEnumerated(STRING)
    @MapKeyColumn(name = "permission", length = 30)
    @Enumerated(STRING)
    @Column(name = "permission_level", length = 10, nullable = false)
    @Builder.Default
    private Map<AdminPermission, AdminPermissionLevel> permissions = new HashMap<>();

    @Column(length = 512)
    private String profileImageUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    public void updateProfileImage(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void updateProfile(String displayName, AdminRole role,
                              Map<AdminPermission, AdminPermissionLevel> permissions) {
        this.displayName = displayName;
        this.role = role;
        this.permissions.clear();
        this.permissions.putAll(permissions);
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void toggle() {
        this.enabled = !this.enabled;
    }
}
