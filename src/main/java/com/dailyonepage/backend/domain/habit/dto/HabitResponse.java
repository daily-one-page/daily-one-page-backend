package com.dailyonepage.backend.domain.habit.dto;

import com.dailyonepage.backend.domain.habit.entity.Habit;
import com.dailyonepage.backend.domain.habit.entity.HabitType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * 습관 응답 DTO
 */
@Schema(description = "습관 응답")
@Getter
@Builder
public class HabitResponse {

    @Schema(description = "습관 ID", example = "1")
    private Long id;

    @Schema(description = "습관 이름", example = "명상하기")
    private String name;

    @Schema(description = "습관 설명", example = "매일 10분 명상하기")
    private String description;

    @Schema(description = "아이콘 (이모지)", example = "🧘")
    private String icon;

    @Schema(description = "습관 타입", example = "PRACTICE")
    private HabitType type;

    @Schema(description = "시스템 습관 여부", example = "false")
    private boolean isSystem;

    public static HabitResponse from(Habit habit) {
        return HabitResponse.builder()
                .id(habit.getId())
                .name(habit.getName())
                .description(habit.getDescription())
                .icon(habit.getIcon())
                .type(habit.getType())
                .isSystem(habit.isSystemHabit())
                .build();
    }
}
