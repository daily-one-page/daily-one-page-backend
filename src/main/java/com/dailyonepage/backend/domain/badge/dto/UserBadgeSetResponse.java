package com.dailyonepage.backend.domain.badge.dto;

import com.dailyonepage.backend.domain.badge.entity.UserBadgeSet;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 사용자 뱃지세트 진행 상황 응답 DTO
 */
@Schema(description = "뱃지세트 진행 상황 응답")
@Getter
@Builder
public class UserBadgeSetResponse {

    @Schema(description = "진행 ID", example = "1")
    private Long id;

    @Schema(description = "뱃지세트 이름", example = "스트릭 도전")
    private String badgeSetName;

    @Schema(description = "현재 도전 중인 뱃지")
    private BadgeResponse currentBadge;

    @Schema(description = "현재 진행 값", example = "5")
    private int currentValue;

    @Schema(description = "다음 뱃지까지 필요한 값", example = "2")
    private int remainingValue;

    @Schema(description = "진행률 (%)", example = "71")
    private int progressPercent;

    @Schema(description = "시작일시", example = "2025-01-22T10:00:00")
    private LocalDateTime startedAt;

    public static UserBadgeSetResponse from(UserBadgeSet userBadgeSet) {
        int conditionValue = userBadgeSet.getCurrentBadge().getConditionValue();
        int currentValue = userBadgeSet.getCurrentValue();
        int remaining = Math.max(0, conditionValue - currentValue);
        int progress = (int) ((double) currentValue / conditionValue * 100);

        return UserBadgeSetResponse.builder()
                .id(userBadgeSet.getId())
                .badgeSetName(userBadgeSet.getBadgeSet().getName())
                .currentBadge(BadgeResponse.from(userBadgeSet.getCurrentBadge()))
                .currentValue(currentValue)
                .remainingValue(remaining)
                .progressPercent(Math.min(100, progress))
                .startedAt(userBadgeSet.getCreatedAt())
                .build();
    }
}
