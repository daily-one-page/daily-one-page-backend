package com.dailyonepage.backend.domain.badge.dto;

import com.dailyonepage.backend.domain.badge.entity.Badge;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * 뱃지 응답 DTO
 */
@Schema(description = "뱃지 응답")
@Getter
@Builder
public class BadgeResponse {

    @Schema(description = "뱃지 ID", example = "1")
    private Long id;

    @Schema(description = "뱃지 이름", example = "7일 연속 달성")
    private String name;

    @Schema(description = "뱃지 설명", example = "7일 연속으로 습관을 달성했습니다!")
    private String description;

    @Schema(description = "달성 조건 값", example = "7")
    private int conditionValue;

    @Schema(description = "순서", example = "1")
    private int sequence;

    @Schema(description = "아이콘", example = "🏅")
    private String icon;

    public static BadgeResponse from(Badge badge) {
        return BadgeResponse.builder()
                .id(badge.getId())
                .name(badge.getName())
                .description(badge.getDescription())
                .conditionValue(badge.getConditionValue())
                .sequence(badge.getSequence())
                .icon(badge.getIcon())
                .build();
    }
}
