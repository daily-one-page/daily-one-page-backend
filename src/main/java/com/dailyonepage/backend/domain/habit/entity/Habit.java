package com.dailyonepage.backend.domain.habit.entity;

import com.dailyonepage.backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 습관 정의 (템플릿)
 *
 * user_id가 null이면 시스템 습관 (모든 사용자가 선택 가능)
 * user_id가 있으면 해당 사용자가 만든 커스텀 습관
 */
@Entity
@Table(name = "habit")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Habit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * null이면 시스템 습관, 값이 있으면 커스텀 습관
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HabitType type;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Habit(User user, String name, HabitType type) {
        this.user = user;
        this.name = name;
        this.type = type;
        this.createdAt = LocalDateTime.now();
    }

    // 시스템 습관인지 확인
    public boolean isSystemHabit() {
        return this.user == null;
    }

    // 커스텀 습관인지 확인
    public boolean isCustomHabit() {
        return this.user != null;
    }
}
