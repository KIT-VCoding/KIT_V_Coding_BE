package com.example.thinkmap.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "세션 제목 수정 요청")
public record UpdateSessionTitleRequest(
        @Schema(description = "새 세션 제목", example = "광합성 심화 탐구")
        @NotBlank(message = "세션 제목은 필수입니다.")
        @Size(max = 255, message = "세션 제목은 255자를 초과할 수 없습니다.")
        String title
) {
    /** 입력값 정규화 (앞뒤 공백 제거) */
    public UpdateSessionTitleRequest {
        if (title != null) title = title.strip();
    }
}
