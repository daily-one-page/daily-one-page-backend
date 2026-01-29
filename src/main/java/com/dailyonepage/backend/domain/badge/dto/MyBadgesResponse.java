package com.dailyonepage.backend.domain.badge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 내 뱃지 목록 응답 DTO
 */
@Schema(description = "내 뱃지 목록 응답")
@Getter
@Builder
public class MyBadgesResponse {

    @Schema(description = "획득한 뱃지 목록")
    private List<UserBadgeResponse> acquired;

    @Schema(description = "진행 중인 뱃지세트 목록")
    private List<UserBadgeSetResponse> inProgress;

    @Schema(description = "총 획득 뱃지 수", example = "5")
    private int totalAcquired;

    public static MyBadgesResponse of(List<UserBadgeResponse> acquired, List<UserBadgeSetResponse> inProgress) {
        return MyBadgesResponse.builder()
                .acquired(acquired)
                .inProgress(inProgress)
                .totalAcquired(acquired.size())
                .build();
    }
}
