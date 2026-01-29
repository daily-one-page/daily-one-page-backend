package com.dailyonepage.backend.domain.badge.dto;

import com.dailyonepage.backend.domain.badge.entity.UserBadge;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 획득한 뱃지 응답 DTO
 */
@Schema(description = "획득한 뱃지 응답")
@Getter
@Builder
public class UserBadgeResponse {

    @Schema(description = "획득 ID", example = "1")
    private Long id;

    @Schema(description = "뱃지 정보")
    private BadgeResponse badge;

    @Schema(description = "뱃지세트 이름", example = "스트릭 도전")
    private String badgeSetName;

    @Schema(description = "획득일시", example = "2025-01-29T10:00:00")
    private LocalDateTime completedAt;

    public static UserBadgeResponse from(UserBadge userBadge) {
        return UserBadgeResponse.builder()
                .id(userBadge.getId())
                .badge(BadgeResponse.from(userBadge.getBadge()))
                .badgeSetName(userBadge.getBadge().getBadgeSet().getName())
                .completedAt(userBadge.getCompletedAt())
                .build();
    }
}
