package com.dailyonepage.backend.domain.habit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 습관 등록 요청 DTO
 */
@Schema(description = "사용자 습관 등록 요청")
@Getter
@NoArgsConstructor
public class UserHabitCreateRequest {

    @Schema(description = "등록할 습관 ID", example = "1")
    @NotNull(message = "습관 ID는 필수입니다.")
    private Long habitId;
}
