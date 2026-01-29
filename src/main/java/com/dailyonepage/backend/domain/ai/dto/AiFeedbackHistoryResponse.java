package com.dailyonepage.backend.domain.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * AI 피드백 히스토리 응답 DTO
 */
@Schema(description = "AI 피드백 히스토리 응답")
@Getter
@Builder
public class AiFeedbackHistoryResponse {

    @Schema(description = "조회 연도", example = "2025")
    private int year;

    @Schema(description = "조회 월", example = "1")
    private int month;

    @Schema(description = "피드백 목록")
    private List<AiFeedbackResponse> feedbacks;

    @Schema(description = "총 개수", example = "15")
    private int totalCount;

    public static AiFeedbackHistoryResponse of(int year, int month, List<AiFeedbackResponse> feedbacks) {
        return AiFeedbackHistoryResponse.builder()
                .year(year)
                .month(month)
                .feedbacks(feedbacks)
                .totalCount(feedbacks.size())
                .build();
    }
}
