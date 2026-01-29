package com.dailyonepage.backend.domain.dailypage.dto;

import com.dailyonepage.backend.domain.dailypage.entity.DailyPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 데일리 페이지 응답 DTO
 */
@Schema(description = "데일리 페이지 응답")
@Getter
@Builder
public class DailyPageResponse {

    @Schema(description = "페이지 ID", example = "1")
    private Long id;

    @Schema(description = "작성 날짜", example = "2025-01-29")
    private LocalDate date;

    @Schema(description = "페이지 내용", example = "오늘 하루도 열심히 달렸다...")
    private String content;

    @Schema(description = "생성일시", example = "2025-01-29T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정일시", example = "2025-01-29T15:00:00")
    private LocalDateTime updatedAt;

    public static DailyPageResponse from(DailyPage dailyPage) {
        return DailyPageResponse.builder()
                .id(dailyPage.getId())
                .date(dailyPage.getDate())
                .content(dailyPage.getContent())
                .createdAt(dailyPage.getCreatedAt())
                .updatedAt(dailyPage.getUpdatedAt())
                .build();
    }
}
