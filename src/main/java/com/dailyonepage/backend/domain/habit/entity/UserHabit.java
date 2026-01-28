package com.dailyonepage.backend.domain.habit.entity;

import com.dailyonepage.backend.domain.user.entity.User;
import com.dailyonepage.backend.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 사용자별 습관
 *
 * 사용자가 선택한 습관과 해당 습관의 스트릭 정보를 관리
 * Habit(템플릿)과 User를 연결하는 중간 테이블 역할
 */
@Entity
@Table(name = "user_habit",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "habit_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserHabit extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habit_id", nullable = false)
    private Habit habit;

    @Column(name = "current_streak", nullable = false)
    private int currentStreak = 0;

    @Column(name = "last_checked_date")
    private LocalDate lastCheckedDate;

    @Builder
    public UserHabit(User user, Habit habit) {
        this.user = user;
        this.habit = habit;
        this.currentStreak = 0;
        this.lastCheckedDate = null;
    }

    /**
     * 습관 체크 시 스트릭 업데이트
     *
     * @param today 오늘 날짜
     */
    public void checkHabit(LocalDate today) {
        if (lastCheckedDate == null) {
            // 첫 체크
            this.currentStreak = 1;
        } else if (lastCheckedDate.equals(today.minusDays(1))) {
            // 연속 체크 (어제 체크했으면)
            this.currentStreak += 1;
        } else if (!lastCheckedDate.equals(today)) {
            // 연속 끊김 (어제가 아니면 리셋)
            this.currentStreak = 1;
        }
        // 같은 날 중복 체크는 무시
        this.lastCheckedDate = today;
    }

    /**
     * 스트릭 재계산 (과거 수정 시 사용)
     */
    public void recalculateStreak(int newStreak, LocalDate lastDate) {
        this.currentStreak = newStreak;
        this.lastCheckedDate = lastDate;
    }

    /**
     * 스트릭 리셋
     */
    public void resetStreak() {
        this.currentStreak = 0;
        this.lastCheckedDate = null;
    }
}
