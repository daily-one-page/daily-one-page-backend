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

    @Schema(description = "습관 타입", example = "PRACTICE")
    private HabitType type;

    @Schema(description = "소유자 ID (null이면 시스템 습관)", example = "1")
    private Long userId;

    public static HabitResponse from(Habit habit) {
        return HabitResponse.builder()
                .id(habit.getId())
                .name(habit.getName())
                .type(habit.getType())
                .userId(habit.getUser() != null ? habit.getUser().getId() : null)
                .build();
    }
}
