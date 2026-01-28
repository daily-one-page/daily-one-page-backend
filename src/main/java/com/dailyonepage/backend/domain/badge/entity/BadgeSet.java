package com.dailyonepage.backend.domain.badge.entity;

import com.dailyonepage.backend.domain.habit.entity.Habit;
import com.dailyonepage.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 뱃지 세트 정의
 *
 * 뱃지를 그룹으로 관리하는 세트
 *
 * 유형 구분:
 * - user_id=null, habit_id=null → 범용 뱃지세트 (모든 습관에 적용, 예: 스트릭 도전)
 * - user_id=null, habit_id=있음 → 시스템 습관 전용 뱃지세트 (예: 금연→절약금액)
 * - user_id=있음 → 사용자 커스텀 뱃지세트 (후순위)
 */
@Entity
@Table(name = "badge_set")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BadgeSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * null이면 시스템 뱃지세트
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * null이면 범용 뱃지세트 (모든 습관에 적용)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habit_id")
    private Habit habit;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "badgeSet", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC")
    private List<Badge> badges = new ArrayList<>();

    @Builder
    public BadgeSet(User user, Habit habit, String name, String description) {
        this.user = user;
        this.habit = habit;
        this.name = name;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    // 범용 뱃지세트인지 (모든 습관에 적용)
    public boolean isUniversal() {
        return this.habit == null && this.user == null;
    }

    // 시스템 뱃지세트인지
    public boolean isSystem() {
        return this.user == null;
    }

    // 뱃지 추가
    public void addBadge(Badge badge) {
        this.badges.add(badge);
    }
}
