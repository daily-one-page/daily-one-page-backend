package com.dailyonepage.backend.domain.habit.dto;

import com.dailyonepage.backend.domain.badge.entity.BadgeSet;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * 뱃지세트 간단 응답 DTO (습관 목록 조회용)
 */
@Schema(description = "뱃지세트 간단 정보")
@Getter
@Builder
public class BadgeSetSimpleResponse {

    @Schema(description = "뱃지세트 ID", example = "1")
    private Long id;

    @Schema(description = "뱃지세트 이름", example = "거리 도전")
    private String name;

    @Schema(description = "뱃지세트 설명", example = "달린 거리로 뱃지 획득")
    private String description;

    public static BadgeSetSimpleResponse from(BadgeSet badgeSet) {
        return BadgeSetSimpleResponse.builder()
                .id(badgeSet.getId())
                .name(badgeSet.getName())
                .description(badgeSet.getDescription())
                .build();
    }
}
