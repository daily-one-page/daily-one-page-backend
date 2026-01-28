package com.dailyonepage.backend.domain.badge.entity;

import com.dailyonepage.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 획득한 뱃지
 *
 * 사용자가 실제로 획득한 뱃지 기록
 */
@Entity
@Table(name = "user_badge",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"user_id", "badge_id", "user_badge_set_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_badge_set_id", nullable = false)
    private UserBadgeSet userBadgeSet;

    /**
     * 뱃지 획득 시점
     */
    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public UserBadge(User user, Badge badge, UserBadgeSet userBadgeSet) {
        this.user = user;
        this.badge = badge;
        this.userBadgeSet = userBadgeSet;
        this.completedAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }
}
