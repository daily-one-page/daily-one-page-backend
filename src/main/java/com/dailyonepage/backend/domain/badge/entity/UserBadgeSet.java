package com.dailyonepage.backend.domain.badge.entity;

import com.dailyonepage.backend.domain.habit.entity.UserHabit;
import com.dailyonepage.backend.domain.user.entity.User;
import com.dailyonepage.backend.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자별 뱃지세트 진행 상황
 *
 * 사용자가 특정 습관에서 뱃지세트를 얼마나 진행했는지 추적
 * 범용 뱃지세트도 습관별로 진행 상황이 분리됨
 */
@Entity
@Table(name = "user_badge_set",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"user_id", "user_habit_id", "badge_set_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBadgeSet extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_habit_id", nullable = false)
    private UserHabit userHabit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_set_id", nullable = false)
    private BadgeSet badgeSet;

    /**
     * 현재 진행 중인 뱃지 (다음에 획득할 뱃지)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_badge_id", nullable = false)
    private Badge currentBadge;

    /**
     * 현재 진행 값 (예: 현재 스트릭 7일)
     */
    @Column(name = "current_value", nullable = false)
    private int currentValue = 0;

    @Builder
    public UserBadgeSet(User user, UserHabit userHabit, BadgeSet badgeSet, Badge currentBadge) {
        this.user = user;
        this.userHabit = userHabit;
        this.badgeSet = badgeSet;
        this.currentBadge = currentBadge;
        this.currentValue = 0;
    }

    /**
     * 진행 값 업데이트
     */
    public void updateProgress(int value) {
        this.currentValue = value;
    }

    /**
     * 다음 뱃지로 이동 (현재 뱃지 획득 후)
     */
    public void moveToNextBadge(Badge nextBadge) {
        this.currentBadge = nextBadge;
        this.currentValue = 0;  // 진행값 리셋
    }

    /**
     * 뱃지 세트 완료 여부
     */
    public boolean isCompleted() {
        return this.currentBadge == null;
    }
}
