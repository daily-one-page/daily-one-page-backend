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

    @Schema(description = "습관 설명", example = "매일 10분 명상하기")
    @Size(max = 500, message = "습관 설명은 500자 이하여야 합니다.")
    private String description;

    @Schema(description = "아이콘 (이모지)", example = "🧘")
    @Size(max = 10, message = "아이콘은 10자 이하여야 합니다.")
    private String icon;

    @Schema(description = "습관 타입", example = "PRACTICE")
    @NotNull(message = "습관 타입은 필수입니다.")
    private HabitType type;
}
