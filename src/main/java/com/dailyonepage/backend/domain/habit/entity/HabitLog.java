package com.dailyonepage.backend.domain.habit.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 습관 체크 기록
 *
 * 날짜별로 습관을 체크했는지 기록
 * 실천습관: checked=true면 성공
 * 절제습관: checked=true면 실패 (유혹에 넘어감)
 */
@Entity
@Table(name = "habit_log",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_habit_id", "date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HabitLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_habit_id", nullable = false)
    private UserHabit userHabit;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private boolean checked;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public HabitLog(UserHabit userHabit, LocalDate date, boolean checked) {
        this.userHabit = userHabit;
        this.date = date;
        this.checked = checked;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 체크 상태 토글
     */
    public void toggle() {
        this.checked = !this.checked;
    }

    /**
     * 체크 상태 변경
     */
    public void updateChecked(boolean checked) {
        this.checked = checked;
    }
}
