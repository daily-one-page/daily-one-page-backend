package com.dailyonepage.backend.domain.dailypage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * 월별 캘린더 응답 DTO
 */
@Schema(description = "월별 캘린더 응답")
@Getter
@Builder
public class CalendarResponse {

    @Schema(description = "조회 연도", example = "2025")
    private int year;

    @Schema(description = "조회 월", example = "1")
    private int month;

    @Schema(description = "작성된 날짜 목록")
    private List<CalendarDay> days;

    @Schema(description = "총 작성 일수", example = "15")
    private int totalDays;

    public static CalendarResponse of(int year, int month, List<CalendarDay> days) {
        return CalendarResponse.builder()
                .year(year)
                .month(month)
                .days(days)
                .totalDays(days.size())
                .build();
    }

    /**
     * 캘린더 날짜 정보
     */
    @Schema(description = "캘린더 날짜 정보")
    @Getter
    @Builder
    public static class CalendarDay {

        @Schema(description = "날짜", example = "2025-01-29")
        private LocalDate date;

        @Schema(description = "페이지 ID", example = "1")
        private Long pageId;

        @Schema(description = "내용 미리보기 (50자)", example = "오늘 하루도 열심히...")
        private String preview;

        public static CalendarDay from(Long pageId, LocalDate date, String content) {
            String preview = content.length() > 50
                    ? content.substring(0, 50) + "..."
                    : content;

            return CalendarDay.builder()
                    .date(date)
                    .pageId(pageId)
                    .preview(preview)
                    .build();
        }
    }
}
