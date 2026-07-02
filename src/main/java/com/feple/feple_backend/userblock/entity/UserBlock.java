package com.feple.feple_backend.userblock.entity;

import com.feple.feple_backend.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_block",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"blocker_id", "blocked_id"})
        },
        indexes = {
                @Index(name = "idx_user_block_blocker_id", columnList = "blocker_id")
        }
)
public class UserBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public Long getBlockerId() { return blocker.getId(); }
    public Long getBlockedId() { return blocked.getId(); }

    public static UserBlock of(User blocker, User blocked) {
        UserBlock b = new UserBlock();
        b.blocker = blocker;
        b.blocked = blocked;
        return b;
    }
}
