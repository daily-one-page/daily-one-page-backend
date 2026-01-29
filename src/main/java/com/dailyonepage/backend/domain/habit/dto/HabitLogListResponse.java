package com.dailyonepage.backend.domain.habit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * 습관 체크 기록 목록 응답 DTO
 */
@Schema(description = "습관 체크 기록 목록 응답")
@Getter
@Builder
public class HabitLogListResponse {

    @Schema(description = "조회 날짜", example = "2025-01-29")
    private LocalDate date;

    @Schema(description = "체크 기록 목록")
    private List<HabitLogResponse> logs;

    @Schema(description = "총 개수", example = "3")
    private int totalCount;

    public static HabitLogListResponse of(LocalDate date, List<HabitLogResponse> logs) {
        return HabitLogListResponse.builder()
                .date(date)
                .logs(logs)
                .totalCount(logs.size())
                .build();
    }
}
