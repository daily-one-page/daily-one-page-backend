package com.dailyonepage.backend.domain.dailypage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 데일리 페이지 생성 요청 DTO
 */
@Schema(description = "데일리 페이지 생성 요청")
@Getter
@NoArgsConstructor
public class DailyPageCreateRequest {

    @Schema(description = "작성 날짜 (null이면 오늘)", example = "2025-01-29")
    private LocalDate date;

    @Schema(description = "페이지 내용", example = "오늘 하루도 열심히 달렸다. 5km를 완주했고...")
    @NotBlank(message = "내용은 필수입니다.")
    @Size(max = 10000, message = "내용은 10000자 이하여야 합니다.")
    private String content;

    /**
     * 날짜가 없으면 오늘 반환
     */
    public LocalDate getDateOrToday() {
        return date != null ? date : LocalDate.now();
    }
}
