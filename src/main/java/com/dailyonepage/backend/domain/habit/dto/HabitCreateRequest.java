package com.dailyonepage.backend.domain.habit.dto;

import com.dailyonepage.backend.domain.habit.entity.HabitType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 커스텀 습관 생성 요청 DTO
 */
@Schema(description = "커스텀 습관 생성 요청")
@Getter
@NoArgsConstructor
public class HabitCreateRequest {

    @Schema(description = "습관 이름", example = "명상하기")
    @NotBlank(message = "습관 이름은 필수입니다.")
    @Size(max = 100, message = "습관 이름은 100자 이하여야 합니다.")
    private String name;

    @Schema(description = "습관 타입", example = "PRACTICE")
    @NotNull(message = "습관 타입은 필수입니다.")
    private HabitType type;
}
