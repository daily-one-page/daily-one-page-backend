package com.dailyonepage.backend.domain.habit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 습관 체크 요청 DTO
 */
@Schema(description = "습관 체크 요청")
@Getter
@NoArgsConstructor
public class HabitLogCreateRequest {

    @Schema(description = "사용자 습관 ID", example = "1")
    @NotNull(message = "사용자 습관 ID는 필수입니다.")
    private Long userHabitId;

    @Schema(description = "체크 날짜 (null이면 오늘)", example = "2025-01-29")
    private LocalDate date;

    @Schema(description = "체크 여부 (기본값: true)", example = "true")
    private Boolean checked;

    /**
     * 날짜가 없으면 오늘 반환
     */
    public LocalDate getDateOrToday() {
        return date != null ? date : LocalDate.now();
    }

    /**
     * checked가 없으면 true 반환
     */
    public boolean isCheckedOrDefault() {
        return checked != null ? checked : true;
    }
}
