package com.dailyonepage.backend.domain.dailypage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 데일리 페이지 수정 요청 DTO
 */
@Schema(description = "데일리 페이지 수정 요청")
@Getter
@NoArgsConstructor
public class DailyPageUpdateRequest {

    @Schema(description = "수정할 내용", example = "오늘 하루도 열심히 달렸다. 5km를 완주했고...")
    @NotBlank(message = "내용은 필수입니다.")
    @Size(max = 10000, message = "내용은 10000자 이하여야 합니다.")
    private String content;
}
