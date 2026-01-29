package com.dailyonepage.backend.domain.habit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 시스템 습관 목록 응답 DTO
 */
@Schema(description = "시스템 습관 목록 응답")
@Getter
@Builder
public class SystemHabitListResponse {

    @Schema(description = "시스템 습관 목록")
    private List<SystemHabitResponse> habits;

    public static SystemHabitListResponse from(List<SystemHabitResponse> habits) {
        return SystemHabitListResponse.builder()
                .habits(habits)
                .build();
    }
}
