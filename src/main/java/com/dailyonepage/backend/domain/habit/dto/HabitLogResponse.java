package com.dailyonepage.backend.domain.habit.dto;

import com.dailyonepage.backend.domain.habit.entity.HabitLog;
import com.dailyonepage.backend.domain.habit.entity.HabitType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 습관 체크 기록 응답 DTO
 */
@Schema(description = "습관 체크 기록 응답")
@Getter
@Builder
public class HabitLogResponse {

    @Schema(description = "로그 ID", example = "1")
    private Long id;

    @Schema(description = "사용자 습관 ID", example = "1")
    private Long userHabitId;

    @Schema(description = "습관 이름", example = "달리기")
    private String habitName;

    @Schema(description = "습관 타입", example = "PRACTICE")
    private HabitType habitType;

    @Schema(description = "체크 날짜", example = "2025-01-29")
    private LocalDate date;

    @Schema(description = "체크 여부", example = "true")
    private boolean checked;

    @Schema(description = "현재 스트릭", example = "7")
    private int currentStreak;

    @Schema(description = "생성일시", example = "2025-01-29T10:00:00")
    private LocalDateTime createdAt;

    public static HabitLogResponse from(HabitLog habitLog) {
        return HabitLogResponse.builder()
                .id(habitLog.getId())
                .userHabitId(habitLog.getUserHabit().getId())
                .habitName(habitLog.getUserHabit().getHabit().getName())
                .habitType(habitLog.getUserHabit().getHabit().getType())
                .date(habitLog.getDate())
                .checked(habitLog.isChecked())
                .currentStreak(habitLog.getUserHabit().getCurrentStreak())
                .createdAt(habitLog.getCreatedAt())
                .build();
    }
}
