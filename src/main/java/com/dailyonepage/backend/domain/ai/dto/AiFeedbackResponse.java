package com.dailyonepage.backend.domain.ai.dto;

import com.dailyonepage.backend.domain.ai.entity.AiFeedback;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * AI 피드백 응답 DTO
 */
@Schema(description = "AI 피드백 응답")
@Getter
@Builder
public class AiFeedbackResponse {

    @Schema(description = "피드백 ID", example = "1")
    private Long id;

    @Schema(description = "피드백 대상 날짜", example = "2025-01-28")
    private LocalDate date;

    @Schema(description = "AI 피드백 메시지", example = "어제 달리기 습관을 완료하셨네요! 7일 연속 달성까지 2일 남았습니다. 화이팅!")
    private String message;

    @Schema(description = "생성일시", example = "2025-01-29T08:00:00")
    private LocalDateTime createdAt;

    public static AiFeedbackResponse from(AiFeedback feedback) {
        return AiFeedbackResponse.builder()
                .id(feedback.getId())
                .date(feedback.getDate())
                .message(feedback.getMessage())
                .createdAt(feedback.getCreatedAt())
                .build();
    }
}
