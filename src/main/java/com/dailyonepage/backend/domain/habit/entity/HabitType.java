package com.dailyonepage.backend.domain.habit.entity;

/**
 * 습관 유형
 *
 * PRACTICE: 실천 습관 (체크 = 성공) - 예: 물 2L 마시기
 * ABSTINENCE: 절제 습관 (체크 = 실패) - 예: 금연
 */
public enum HabitType {
    PRACTICE,   // 실천습관: 체크하면 성공
    ABSTINENCE  // 절제습관: 체크 안 하면 성공
}
