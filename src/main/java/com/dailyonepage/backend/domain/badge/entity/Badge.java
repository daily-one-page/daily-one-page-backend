package com.dailyonepage.backend.domain.badge.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 개별 뱃지 정의
 *
 * BadgeSet 내에서 순서(sequence)대로 순차 달성
 * 예: 금연 세트 → 치킨(1일) → 오마카세(30일) → 에어팟(100일)
 */
@Entity
@Table(name = "badge")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_set_id", nullable = false)
    private BadgeSet badgeSet;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    /**
     * 달성 조건 값 (예: 7일, 30일, 100일)
     */
    @Column(name = "condition_value", nullable = false)
    private int conditionValue;

    /**
     * 세트 내 순서 (1, 2, 3...)
     */
    @Column(nullable = false)
    private int sequence;

    /**
     * 뱃지 아이콘 (URL 또는 이모지)
     */
    @Column(length = 255)
    private String icon;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Badge(BadgeSet badgeSet, String name, String description,
                 int conditionValue, int sequence, String icon) {
        this.badgeSet = badgeSet;
        this.name = name;
        this.description = description;
        this.conditionValue = conditionValue;
        this.sequence = sequence;
        this.icon = icon;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 조건 달성 여부 확인
     */
    public boolean isAchieved(int currentValue) {
        return currentValue >= this.conditionValue;
    }
}
