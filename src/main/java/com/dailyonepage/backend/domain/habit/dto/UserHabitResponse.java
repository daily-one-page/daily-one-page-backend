package com.dailyonepage.backend.domain.habit.dto;

import com.dailyonepage.backend.domain.habit.entity.HabitType;
import com.dailyonepage.backend.domain.habit.entity.UserHabit;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 사용자 습관 응답 DTO
 */
@Schema(description = "사용자 습관 응답")
@Getter
@Builder
public class UserHabitResponse {

    @Schema(description = "사용자 습관 ID", example = "1")
    private Long id;

    @Schema(description = "습관 ID", example = "1")
    private Long habitId;

    @Schema(description = "습관 이름", example = "달리기")
    private String habitName;

    @Schema(description = "습관 타입", example = "PRACTICE")
    private HabitType habitType;

    @Schema(description = "현재 스트릭", example = "7")
    private int currentStreak;

    @Schema(description = "마지막 체크 날짜", example = "2025-01-29")
    private LocalDate lastCheckedDate;

    @Schema(description = "등록일시", example = "2025-01-01T10:00:00")
    private LocalDateTime createdAt;

    public static UserHabitResponse from(UserHabit userHabit) {
        return UserHabitResponse.builder()
                .id(userHabit.getId())
                .habitId(userHabit.getHabit().getId())
                .habitName(userHabit.getHabit().getName())
                .habitType(userHabit.getHabit().getType())
                .currentStreak(userHabit.getCurrentStreak())
                .lastCheckedDate(userHabit.getLastCheckedDate())
                .createdAt(userHabit.getCreatedAt())
                .build();
    }
}
