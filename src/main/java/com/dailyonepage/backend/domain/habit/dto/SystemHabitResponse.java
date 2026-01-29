package com.dailyonepage.backend.domain.habit.dto;

import com.dailyonepage.backend.domain.badge.entity.BadgeSet;
import com.dailyonepage.backend.domain.habit.entity.Habit;
import com.dailyonepage.backend.domain.habit.entity.HabitType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 시스템 습관 응답 DTO (뱃지세트 포함)
 */
@Schema(description = "시스템 습관 응답 (뱃지세트 포함)")
@Getter
@Builder
public class SystemHabitResponse {

    @Schema(description = "습관 ID", example = "1")
    private Long id;

    @Schema(description = "습관 이름", example = "달리기")
    private String name;

    @Schema(description = "습관 타입", example = "PRACTICE")
    private HabitType type;

    @Schema(description = "연결된 뱃지세트 목록")
    private List<BadgeSetSimpleResponse> badgeSets;

    public static SystemHabitResponse of(Habit habit, List<BadgeSet> badgeSets) {
        return SystemHabitResponse.builder()
                .id(habit.getId())
                .name(habit.getName())
                .type(habit.getType())
                .badgeSets(badgeSets.stream()
                        .map(BadgeSetSimpleResponse::from)
                        .toList())
                .build();
    }
}
