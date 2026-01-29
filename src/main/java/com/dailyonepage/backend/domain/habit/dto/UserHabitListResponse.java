package com.dailyonepage.backend.domain.habit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 사용자 습관 목록 응답 DTO
 */
@Schema(description = "사용자 습관 목록 응답")
@Getter
@Builder
public class UserHabitListResponse {

    @Schema(description = "등록된 습관 목록")
    private List<UserHabitResponse> habits;

    @Schema(description = "총 개수", example = "3")
    private int totalCount;

    public static UserHabitListResponse from(List<UserHabitResponse> habits) {
        return UserHabitListResponse.builder()
                .habits(habits)
                .totalCount(habits.size())
                .build();
    }
}
