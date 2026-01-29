package com.dailyonepage.backend.domain.badge.dto;

import com.dailyonepage.backend.domain.badge.entity.BadgeSet;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 뱃지세트 응답 DTO
 */
@Schema(description = "뱃지세트 응답")
@Getter
@Builder
public class BadgeSetResponse {

    @Schema(description = "뱃지세트 ID", example = "1")
    private Long id;

    @Schema(description = "뱃지세트 이름", example = "스트릭 도전")
    private String name;

    @Schema(description = "뱃지세트 설명", example = "연속 달성 일수로 뱃지를 획득하세요!")
    private String description;

    @Schema(description = "범용 여부 (모든 습관에 적용)", example = "true")
    private boolean universal;

    @Schema(description = "연결된 습관 ID (null이면 범용)", example = "1")
    private Long habitId;

    @Schema(description = "포함된 뱃지 목록")
    private List<BadgeResponse> badges;

    public static BadgeSetResponse from(BadgeSet badgeSet) {
        return BadgeSetResponse.builder()
                .id(badgeSet.getId())
                .name(badgeSet.getName())
                .description(badgeSet.getDescription())
                .universal(badgeSet.isUniversal())
                .habitId(badgeSet.getHabit() != null ? badgeSet.getHabit().getId() : null)
                .badges(badgeSet.getBadges().stream()
                        .map(BadgeResponse::from)
                        .toList())
                .build();
    }
}
