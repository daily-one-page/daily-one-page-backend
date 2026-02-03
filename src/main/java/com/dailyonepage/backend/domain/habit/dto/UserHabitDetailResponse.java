package com.dailyonepage.backend.domain.habit.dto;

import com.dailyonepage.backend.domain.habit.entity.HabitType;
import com.dailyonepage.backend.domain.habit.entity.UserHabit;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 사용자 습관 상세 응답 DTO
 */
@Schema(description = "사용자 습관 상세 응답")
@Getter
@Builder
public class UserHabitDetailResponse {

    @Schema(description = "사용자 습관 ID", example = "1")
    private Long id;

    @Schema(description = "습관 정보")
    private HabitInfo habit;

    @Schema(description = "현재 스트릭", example = "7")
    private int currentStreak;

    @Schema(description = "마지막 체크 날짜", example = "2025-01-29")
    private LocalDate lastCheckedDate;

    @Schema(description = "등록일시", example = "2025-01-01T10:00:00")
    private LocalDateTime createdAt;

    @Getter
    @Builder
    public static class HabitInfo {
        @Schema(description = "습관 ID", example = "1")
        private Long id;

        @Schema(description = "습관 이름", example = "달리기")
        private String name;

        @Schema(description = "습관 설명", example = "매일 30분 달리기")
        private String description;

        @Schema(description = "아이콘 (이모지)", example = "🏃")
        private String icon;

        @Schema(description = "습관 타입", example = "PRACTICE")
        private HabitType type;

        @Schema(description = "시스템 습관 여부", example = "true")
        private boolean isSystem;
    }

    public static UserHabitDetailResponse from(UserHabit userHabit) {
        return UserHabitDetailResponse.builder()
                .id(userHabit.getId())
                .habit(HabitInfo.builder()
                        .id(userHabit.getHabit().getId())
                        .name(userHabit.getHabit().getName())
                        .description(userHabit.getHabit().getDescription())
                        .icon(userHabit.getHabit().getIcon())
                        .type(userHabit.getHabit().getType())
                        .isSystem(userHabit.getHabit().isSystemHabit())
                        .build())
                .currentStreak(userHabit.getCurrentStreak())
                .lastCheckedDate(userHabit.getLastCheckedDate())
                .createdAt(userHabit.getCreatedAt())
                .build();
    }
}
